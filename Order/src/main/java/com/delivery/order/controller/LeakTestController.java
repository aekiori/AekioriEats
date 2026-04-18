package com.delivery.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leak-test")
public class LeakTestController {

    private static final List<byte[]> LEAK = new ArrayList<>();

    @PostMapping("/heap")
    public Map<String, Object> leakHeap(
        @RequestParam(defaultValue = "10") int mb
    ) {
        byte[] block = new byte[mb * 1024 * 1024];
        LEAK.add(block);

        return Map.of(
            "leakedMbThisRequest", mb,
            "entries", LEAK.size(),
            "approxLeakedMb", LEAK.size() * mb
        );
    }

    @PostMapping("/clear")
    public Map<String, Object> clear() {
        int count = LEAK.size();
        LEAK.clear();
        System.gc();

        return Map.of(
            "clearedEntries", count
        );
    }
}
