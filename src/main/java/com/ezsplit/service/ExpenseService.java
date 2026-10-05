package com.ezsplit.service;

import com.ezsplit.dto.request.CreateExpenseRequest;
import com.ezsplit.dto.response.ExpenseResponse;
import com.ezsplit.entity.Expense;
import com.ezsplit.entity.ExpenseSplit;
import com.ezsplit.entity.Group;
import com.ezsplit.entity.User;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpenseResponse create(UUID groupId, CreateExpenseRequest req, UUID callerUserId) {
        Group group = requireGroup(groupId);
        requireMembership(groupId, callerUserId);

        User payer = userRepository.findById(req.getPaidByUserId())
                .orElseThrow(() -> new BusinessException("Payer not found"));
        User creator = userRepository.findById(callerUserId)
                .orElseThrow(() -> new BusinessException("Creator not found"));

        Map<UUID, BigDecimal> shares = validateAndCompute(groupId, req, callerUserId);

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(payer)
                .createdBy(creator)
                .description(req.getDescription().trim())
                .total(req.getTotal().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .currency(group.getCurrency())
                .category(req.getCategory())
                .splitMethod(req.getSplitMethod())
                .build();

        Expense saved = expenseRepository.saveAndFlush(expense);
        persistSplits(saved, shares);

        return toResponse(saved, expenseSplitRepository.findAllByExpenseExpenseId(saved.getExpenseId()));
    }

    /**
     * Updates an existing expense. Recomputes and replaces all splits.
     * Uses optimistic locking via {@code Expense.version} to detect concurrent edits.
     *
     * @param groupId   the group the expense belongs to
     * @param expenseId the expense to update
     * @param req       the new values
     * @param callerUserId the authenticated user making the request
     * @return the updated expense
     * @throws BusinessException if the expense doesn't exist, the caller isn't a member,
     *                           or the request fails validation
     * @throws OptimisticLockingFailureException if the expense was modified by someone
     *                           else between read and write
     */
    @Transactional
    public ExpenseResponse update(UUID groupId,
                                  UUID expenseId,
                                  CreateExpenseRequest req,
                                  UUID callerUserId) {
        requireMembership(groupId, callerUserId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException("Expense not found"));

        if (!expense.getGroup().getGroupId().equals(groupId)) {
            throw new BusinessException("Expense does not belong to this group");
        }

        Map<UUID, BigDecimal> newShares = validateAndCompute(groupId, req, callerUserId);

        User newPayer = userRepository.findById(req.getPaidByUserId())
                .orElseThrow(() -> new BusinessException("Payer not found"));

        // update expense fields
        expense.setDescription(req.getDescription().trim());
        expense.setTotal(req.getTotal().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        expense.setCategory(req.getCategory());
        expense.setSplitMethod(req.getSplitMethod());
        expense.setPaidBy(newPayer);

        // delete old splits, insert new ones
        expenseSplitRepository.deleteAllByExpenseExpenseId(expenseId);

        // saveAndFlush triggers the version check immediately
        Expense saved = expenseRepository.saveAndFlush(expense);

        persistSplits(saved, newShares);

        return toResponse(saved, expenseSplitRepository.findAllByExpenseExpenseId(saved.getExpenseId()));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listForGroup(UUID groupId, UUID callerUserId) {
        requireMembership(groupId, callerUserId);

        List<Expense> expenses =
                expenseRepository.findAllByGroupGroupIdOrderByCreatedAtDesc(groupId);

        if (expenses.isEmpty()) return List.of();

        List<UUID> ids = expenses.stream().map(Expense::getExpenseId).toList();
        List<ExpenseSplit> allSplits = expenseSplitRepository.findAllWithUserByExpenseIds(ids);

        Map<UUID, List<ExpenseSplit>> byExpense = new HashMap<>();
        for (ExpenseSplit s : allSplits) {
            byExpense.computeIfAbsent(s.getExpense().getExpenseId(), k -> new ArrayList<>()).add(s);
        }

        return expenses.stream()
                .map(e -> toResponse(e, byExpense.getOrDefault(e.getExpenseId(), List.of())))
                .toList();
    }

    private Map<UUID, BigDecimal> computeShares(CreateExpenseRequest req) {
        return switch (req.getSplitMethod()) {
            case EQUAL      -> computeEqual(req.getParticipants(), req.getTotal());
            case PERCENTAGE -> computePercentage(req.getParticipants(), req.getTotal());
            case CUSTOM     -> computeCustom(req.getParticipants(), req.getTotal());
        };
    }

    private Map<UUID, BigDecimal> computeEqual(
            List<CreateExpenseRequest.ParticipantShare> participants,
            BigDecimal total) {

        List<UUID> ids = participants.stream()
                .map(CreateExpenseRequest.ParticipantShare::getUserId)
                .distinct()
                .toList();

        BigDecimal count = BigDecimal.valueOf(ids.size());
        BigDecimal base = total.divide(count, MONEY_SCALE, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(base.multiply(count));

        Map<UUID, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            BigDecimal amount = (i == ids.size() - 1) ? base.add(remainder) : base;
            result.put(ids.get(i), amount);
        }
        return result;
    }

    private Map<UUID, BigDecimal> computePercentage(
            List<CreateExpenseRequest.ParticipantShare> participants,
            BigDecimal total) {
        BigDecimal pctSum = BigDecimal.ZERO;
        for (var p : participants) {
            if (p.getPercentage() == null) {
                throw new BusinessException("Percentage is required for each participant");
            }
            if (p.getPercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Percentage must be greater than zero");
            }
            pctSum = pctSum.add(p.getPercentage());
        }

        if (pctSum.compareTo(HUNDRED) != 0) {
            throw new BusinessException("Percentages must sum to 100, got " + pctSum);
        }

        Map<UUID, BigDecimal> result = new LinkedHashMap<>();
        for (var p : participants) {
            BigDecimal amount = total
                    .multiply(p.getPercentage())
                    .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
            result.put(p.getUserId(), amount);
        }

        return fixRounding(result, total);
    }

    private Map<UUID, BigDecimal> computeCustom(
            List<CreateExpenseRequest.ParticipantShare> participants,
            BigDecimal total) {

        Map<UUID, BigDecimal> result = new LinkedHashMap<>();

        for (var p : participants) {
            if (p.getAmount() == null) {
                throw new BusinessException("Amount is required for each participant in a CUSTOM split");
            }
            if (p.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Each share must be greater than zero");
            }
            result.put(p.getUserId(),
                    p.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }

        BigDecimal sum = result.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expected = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (sum.compareTo(expected) != 0) {
            throw new BusinessException(
                    "Custom amounts must sum to the total (" + expected + "), got " + sum);
        }

        return result;
    }

    /**
     * Validates an expense request against the group and computes the split.
     * Shared between create and update.
     *
     * @return the computed per-user amounts, keyed by user ID
     */
    private Map<UUID, BigDecimal> validateAndCompute(
            UUID groupId,
            CreateExpenseRequest req,
            UUID callerUserId) {
        User payer = userRepository.findById(req.getPaidByUserId())
                .orElseThrow(() -> new BusinessException("Payer not found"));
        requireMembership(groupId, payer.getUserId());

        if (req.getParticipants() == null || req.getParticipants().isEmpty()) {
            throw new BusinessException("At least one participant is required");
        }

        for (var p : req.getParticipants()) {
            requireMembership(groupId, p.getUserId());
        }

        BigDecimal total = req.getTotal().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total must be greater than zero");
        }

        return computeShares(req);
    }

    private void persistSplits(Expense expense, Map<UUID, BigDecimal> shares) {
        List<ExpenseSplit> splits = new ArrayList<>();
        for (var entry : shares.entrySet()) {
            User participant = userRepository.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException("Participant not found"));
            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(participant)
                    .amountOwed(entry.getValue())
                    .build());
        }
        expenseSplitRepository.saveAll(splits);
    }

    /**
     * Adjusts the computed split amounts so their sum exactly equals the total.
     *
     * This method is only used for EQUAL and PERCENTAGE SPLITS.
     * Equal and percentage splits involve division, which can leave rounding drift
     * of a cent or two. For example, splitting $10.00 three ways yields three shares
     * of $3.33, summing to $9.99 — one cent short of the total. This method finds
     * that difference and assigns it to the last participant in iteration order,
     * guaranteeing the invariant that the sum(amounts) should always equal to total.
     *
     * @param amounts  map of userId → amount owed, computed by the split method
     * @param total    the expense total that the amounts must sum to
     * @return the same map, adjusted so that the values sum exactly to {@code total}
     */
    private Map<UUID, BigDecimal> fixRounding(Map<UUID, BigDecimal> amounts, BigDecimal total) {
        BigDecimal sum = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP).subtract(sum);

        if (diff.compareTo(BigDecimal.ZERO) == 0) return amounts;

        UUID last = null;
        for (UUID k : amounts.keySet()) last = k;
        if (last != null) {
            amounts.put(last, amounts.get(last).add(diff));
        }
        return amounts;
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────
    private Group requireGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));
    }

    private void requireMembership(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, userId)) {
            throw new BusinessException("User " + userId + " is not a member of this group");
        }
    }

    private ExpenseResponse toResponse(Expense expense, List<ExpenseSplit> splits) {
        return ExpenseResponse.builder()
                .expenseId(expense.getExpenseId())
                .groupId(expense.getGroup().getGroupId())
                .description(expense.getDescription())
                .total(expense.getTotal())
                .currency(expense.getCurrency())
                .category(expense.getCategory())
                .splitMethod(expense.getSplitMethod())
                .paidByUserId(expense.getPaidBy().getUserId())
                .paidByUsername(expense.getPaidBy().getUsername())
                .createdAt(expense.getCreatedAt())
                .shares(splits.stream()
                        .map(s -> ExpenseResponse.Share.builder()
                                .userId(s.getUser().getUserId())
                                .username(s.getUser().getUsername())
                                .amountOwed(s.getAmountOwed())
                                .build())
                        .toList())
                .build();
    }
}
