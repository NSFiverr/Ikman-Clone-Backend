package com.marketplace.platform.domain.advertisement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdAttributeKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ad_id")
    private Long adId;

    @Column(name = "attr_def_id")
    private Long attrDefId;
}