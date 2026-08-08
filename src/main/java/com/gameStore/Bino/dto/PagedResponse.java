package com.gameStore.Bino.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable JSON shape for paginated endpoints — wraps Spring Data's Page<T> so the
 * API contract is decoupled from PageImpl.
 *
 * Spring Boot 3.3 explicitly warns against returning Page<T> from a controller
 * because PageImpl's serialisation "is not supported to guard against
 * unintentional API breakage" — its shape may shift between versions. Returning
 * PagedResponse<T> instead gives the frontend a small, predictable envelope
 * we own and Spring can't rearrange under us.
 *
 * Fields chosen for what a UI actually needs: content, current page, size,
 * and totals for a "showing N of M" caption and a page counter.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * Map a Page<E> to a PagedResponse<T> via the supplied entity→DTO function.
     * Keeps the paging metadata untouched; only the row shape changes.
     */
    public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
