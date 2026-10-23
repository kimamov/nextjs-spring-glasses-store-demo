package com.visionaids.store.catalog;

import java.util.List;

public record ProductFilterOptionsDto(List<TypeOption> types, List<String> brands, List<String> shapes) {
    public record TypeOption(String value, String label) {
    }
}
