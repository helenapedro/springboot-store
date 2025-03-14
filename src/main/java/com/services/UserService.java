package com.services;

import com.entities.*;
import com.repositories.AddressRepository;
import com.repositories.ProfileRepository;
import com.repositories.UserRepository;
import com.repositories.specifications.ProductSpec;
import com.exception.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void showRelatedEntities(Long profileId) throws EntityNotFoundException {
        validateProfileId(profileId);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + profileId));
        logger.info("User email: {}", profile.getUser().getEmail());
    }

    public void fetchAddress(Long addressId) throws EntityNotFoundException {
        var address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + addressId));
        logger.info("Address: {}", address);
    }

    @Transactional
    public void deleteRelated(Long userId) throws EntityNotFoundException {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        var address = user.getAddresses().get(0);
        user.removeAddress(address);
        // userRepository.save(user); // Uncomment if you want to save the user after
        // removing the address
    }

    @Transactional
    public void manageProducts(Long productId) {
        productRepository.deleteById(productId);
    }

    @Transactional
    public void updateProductPrices(BigDecimal price, byte category) {
        productRepository.updatePriceByCategory(price, category);
    }

    @Transactional
    public List<Product> fetchProducts() {
        var product = new Product();
        product.setName("product");

        var matcher = ExampleMatcher.matching()
                .withIncludeNullValues()
                .withIgnorePaths("id", "description")
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        var example = Example.of(product, matcher);

        var products = productRepository.findAll(example);
        products.forEach(p -> logger.info("Product: {}", p));
        return products;
    }

    public List<Product> fetchProductsByCriteria() {
        var products = productRepository.findProductsByCriteria("prod", BigDecimal.valueOf(1), null);
        products.forEach(p -> logger.info("Product: {}", p));
        return products;
    }

    public List<Product> fetchProductsBySpecifications(String name, BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<Product> spec = Stream.of(
                Optional.ofNullable(name).map(ProductSpec::hasName),
                Optional.ofNullable(minPrice).map(ProductSpec::hasPriceGreaterThanOrEqualTo),
                Optional.ofNullable(maxPrice).map(ProductSpec::hasPriceLessThanOrEqualTo)).filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(Specification::and)
                .orElse(Specification.where(null));

        var products = productRepository.findAll(spec);
        products.forEach(p -> logger.info("Product: {}", p));
        return products;
    }

    @Transactional
    public List<User> fetchUsers() {
        var users = userRepository.findAllWithTags();
        users.forEach(u -> {
            logger.info("User: {}", u);
            u.getAddresses().forEach(a -> logger.info("Address: {}", a));
        });
        return users;
    }

    @Transactional
    public Page<Product> fetchPagedProducts(int page, int size, Sort sort) {
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAll(pageRequest);

        productPage.getContent().forEach(p -> logger.info("Product: {}", p));
        logger.info("Total Pages: {}", productPage.getTotalPages());
        logger.info("Total Elements: {}", productPage.getTotalElements());

        return productPage;
    }

    private void validateProfileId(Long profileId) {
        if (profileId == null || profileId <= 0) {
            throw new IllegalArgumentException("Invalid profile ID: " + profileId);
        }
    }
}