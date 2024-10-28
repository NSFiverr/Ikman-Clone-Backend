package com.marketplace.platform.domain.search;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchIndexKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ad_id")
    private Long adId;

    @Column(name = "attr_def_id")
    private Long attrDefId;
}