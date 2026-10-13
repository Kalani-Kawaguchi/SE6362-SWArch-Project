package com.se6362.kwic.index;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KwicLineRepository extends JpaRepository<KwicLine, Long>
{
    List<KwicLine> findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc(String prefix);

    void deleteByUriIn(Collection<String> uris);
}
