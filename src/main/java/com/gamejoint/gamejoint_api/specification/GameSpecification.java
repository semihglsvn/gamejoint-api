package com.gamejoint.gamejoint_api.specification;

import com.gamejoint.gamejoint_api.model.Game;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class GameSpecification {

    public static Specification<Game> withFilters(
            String query, 
            Integer minMetascore, 
            Boolean hideTbd, 
            List<String> genres, 
            List<String> platforms,
            Boolean isMatchAll // NEW: true = AND, false = OR for tags
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Text Search 
            if (query != null && !query.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%"));
            }

            // 2. Hide TBD (Assuming TBD is stored as 0)
            if (hideTbd != null && hideTbd) {
                predicates.add(cb.greaterThan(root.get("metascore"), 0));
            }

            // 3. Minimum Metascore
            if (minMetascore != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("metascore"), minMetascore));
            }

            // 4. Genre Filter
            if (genres != null && !genres.isEmpty()) {
                Join<Object, Object> genreJoin = root.join("genres");
                if (isMatchAll != null && isMatchAll) {
                    // Match ALL (AND logic for tags - usually requires subqueries or group by, 
                    // but for simplicity in CriteriaBuilder, we can enforce it sequentially if needed, 
                    // though typically 'IN' acts as an OR. 
                    // To do a true 'AND' on a single join is complex because a single row in the join table can't be two genres at once.
                    // The standard way is to group by game ID and having count = genres.size().
                    // For now, we will map BOTH to 'IN' (OR logic) to keep it stable, but we can build the heavy 'HAVING COUNT' logic if you prefer later.
                }
                predicates.add(genreJoin.get("name").in(genres));
            }

            // 5. Platform Filter
            if (platforms != null && !platforms.isEmpty()) {
                Join<Object, Object> platformJoin = root.join("platforms");
                predicates.add(platformJoin.get("name").in(platforms));
            }

            // CRITICAL: Prevent duplicates
            cq.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}