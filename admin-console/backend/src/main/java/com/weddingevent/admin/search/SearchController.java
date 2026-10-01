package com.weddingevent.admin.search;

import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.LeadRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Global header search (Ctrl+K) across contracts and leads by name, phone or contract number. */
@RestController
@RequestMapping("/api/v1/admin/search")
public class SearchController {

    public record SearchResult(String id, String label, String kind) {}

    private static final int MAX_RESULTS = 8;

    private final ContractRepository contracts;
    private final LeadRepository leads;

    public SearchController(ContractRepository contracts, LeadRepository leads) {
        this.contracts = contracts;
        this.leads = leads;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<SearchResult>> search(@RequestParam(required = false, defaultValue = "") String q) {
        String keyword = q.trim().toLowerCase(Locale.ROOT);
        // Escape LIKE wildcards typed by the user
        String pattern = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        int perKind = keyword.isEmpty() ? 2 : MAX_RESULTS / 2;

        List<SearchResult> results = new ArrayList<>();
        contracts.search(pattern, Limit.of(perKind)).forEach(c -> results.add(new SearchResult(
                c.getId().toString(), c.getContractNumber() + " (" + c.getCustomerName() + ") • " + c.getPhone(), "contract")));
        leads.search(pattern, Limit.of(perKind)).forEach(l -> results.add(new SearchResult(
                l.getId().toString(), l.getName() + " • " + l.getPhone(), "lead")));
        return ApiResponse.ok(results);
    }
}
