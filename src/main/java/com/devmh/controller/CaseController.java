package com.devmh.controller;

import com.devmh.model.Case;
import com.devmh.persistence.CaseRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cases")
class CaseController {
    private final CaseRepository caseRepository;

    public CaseController(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @PostMapping
    public Case createCase(@RequestBody Case newCase) {
        return caseRepository.save(newCase);
    }

    @GetMapping
    public List<Case> getAllCases() {
        return (List<Case>) caseRepository.findAll();
    }
}
