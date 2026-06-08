package com.plog.api.domain.recommend.dto;

import lombok.Builder;

@Builder
public record PlaceDetailDto(
        String tel,
        String overview,
        String usetime,
        String usefee,
        String restdate,
        String eventstartdate,
        String eventenddate
) {}