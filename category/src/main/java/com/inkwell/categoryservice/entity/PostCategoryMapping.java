package com.inkwell.categoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_category_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_category", columnNames = {"postId", "categoryId"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCategoryMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long categoryId;
}
