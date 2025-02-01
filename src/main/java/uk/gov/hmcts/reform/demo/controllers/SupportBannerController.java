package uk.gov.hmcts.reform.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.reform.demo.entities.SupportBanner;
import uk.gov.hmcts.reform.demo.repositories.SupportBannerRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/support-banner")
public class SupportBannerController {

    private final SupportBannerRepository supportBannerRepository;

    public SupportBannerController(SupportBannerRepository supportBannerRepository) {
        this.supportBannerRepository = supportBannerRepository;
    }

    /**
     * Fetch all support banners.
     */
    @GetMapping("/")
    public ResponseEntity<List<SupportBanner>> getAllBanners() {
        return ResponseEntity.ok(supportBannerRepository.findAll());
    }

    /**
     * Fetch a support banner by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBannerById(@PathVariable Long id) {
        Optional<SupportBanner> banner = supportBannerRepository.findById(id);
        return banner.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new support banner.
     */
    @PostMapping("/create")
    public ResponseEntity<SupportBanner> createBanner(@RequestBody SupportBanner banner) {
        SupportBanner savedBanner = supportBannerRepository.save(banner);
        return ResponseEntity.ok(savedBanner);
    }

    /**
     * Update a support banner.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SupportBanner> updateBanner(@PathVariable Long id, @RequestBody SupportBanner updatedBanner) {
        Optional<SupportBanner> optionalBanner = supportBannerRepository.findById(id);
        if (!optionalBanner.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        SupportBanner banner = optionalBanner.get();
        banner.setTitle(updatedBanner.getTitle());
        banner.setContent(updatedBanner.getContent());
        supportBannerRepository.save(banner);

        return ResponseEntity.ok(banner);
    }

    /**
     * Delete a support banner.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        Optional<SupportBanner> optionalBanner = supportBannerRepository.findById(id);
        if (!optionalBanner.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        supportBannerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
