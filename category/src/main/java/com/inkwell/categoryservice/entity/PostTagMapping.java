package com.inkwell.categoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_tag_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_tag", columnNames = {"postId", "tagId"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTagMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long tagId;
}
