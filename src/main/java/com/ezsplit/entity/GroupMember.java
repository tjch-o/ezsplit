package com.ezsplit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "group_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_member_group_user",
                columnNames = {"group_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID membershipId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "group_id", nullable = false)
        private Group group;

        @Enumerated(EnumType.STRING)
        @Column(name = "role", nullable = false, length = 20)
        private MembershipRole role;

        @CreationTimestamp
        @Column(name = "joined_at", updatable = false)
        private Instant joinedAt;
}
