package ir.darkdeveloper.anbarinoo.service;

import ir.darkdeveloper.anbarinoo.exception.*;
import ir.darkdeveloper.anbarinoo.model.Financial.BuyModel;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.repository.ProductRepository;
import ir.darkdeveloper.anbarinoo.service.Financial.BuyService;
import ir.darkdeveloper.anbarinoo.util.IOUtils;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import ir.darkdeveloper.anbarinoo.util.ProductUtils;
import lombok.AllArgsConstructor;
import org.hibernate.exception.DataException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final IOUtils ioUtils;
    private final ProductUtils productUtils;
    private final BuyService buyService;
    private final JwtUtils jwtUtils;

    /**
     * saves a new product to the user id of refresh token
     * if image files are null, then sets a default image
     * if not, saves files and sets images
     *
     * @param product id should be null
     * @param req     should contain refresh token
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER') && #product.getCategory() != null")
    public ProductModel saveProduct(ProductModel product, HttpServletRequest req) {
        return exceptionHandlers(() -> {
            ProductModel savedProduct;
            try {
                savedProduct = productUtils.saveProduct(Optional.ofNullable(product), req);
            } catch (IOException e) {
                throw new InternalServerException(e.getLocalizedMessage());
            }
            var buy = BuyModel.builder()
                    .product(savedProduct)
                    .count(savedProduct.getTotalCount())
                    .price(savedProduct.getPrice())
                    .tax(savedProduct.getTax())
                    .build();
            buyService.saveBuy(Optional.of(buy), true, req);
            savedProduct.setFirstBuyId(buy.getId());
            return repo.save(savedProduct);
        });
    }

    /**
     * For regular update with no images: another users can't update, not users who owned products
     * If images and files and id provided, then they will be ignored
     * If price or count value is going to update, it will also update the first buy record of product
     *
     * @param product   should files and id and user be null
     * @param productId should not to be null
     * @param req       should contain refresh token
     * @return updated product with kept images
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public ProductModel updateProduct(Optional<ProductModel> product, Long productId, HttpServletRequest req) {
        return exceptionHandlers(() -> {
            product.map(ProductModel::getId).ifPresent(id -> product.get().setId(null));

            var foundProduct = repo.findById(productId)
                    .orElseThrow(() -> new NoContentException("This product does not exist"));
            productUtils.checkUserIsSameUserForRequest(foundProduct.getCategory().getUser().getId(),
                    req, "update");

            if (product.map(ProductModel::getPrice).isPresent()
                    || product.map(ProductModel::getTotalCount).isPresent())
                productUtils.updateBuyWithProductUpdate(product, foundProduct, buyService, req);

            product.orElseThrow(() -> new BadRequestException("Product can't be null"));
            return productUtils.updateProduct(product.get(), foundProduct);
        });
    }

    /**
     * If a sell or buy record gets updated, product model of that will be updated to
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public void updateProductFromBuyOrSell(Optional<ProductModel> product, ProductModel preProduct, HttpServletRequest req) {
        exceptionHandlers(() -> {
            product.map(ProductModel::getId).ifPresent(id -> product.get().setId(null));
            productUtils.checkUserIsSameUserForRequest(preProduct.getCategory().getUser().getId(), req, "update");
            product.orElseThrow(() -> new BadRequestException("Product can't be null"));
            productUtils.updateProduct(product.get(), preProduct);
            return null;
        });
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public Page<ProductModel> findByNameContains(String name, Pageable pageable, HttpServletRequest req) {
        return exceptionHandlers(() -> {
            var userId = jwtUtils.getUserId(req.getHeader("refresh_token"));
            var foundData = repo
                    .findByNameContainsAndCategoryUserId(name, userId, pageable);
            foundData.map(Page::getContent)
                    .map(list -> list.isEmpty() ? null : list.get(0))
                    .orElseThrow(() -> new NoContentException("This product does not exist"));
            return foundData.get();
        });
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public ProductModel getProduct(Long productId, HttpServletRequest req) {
        return exceptionHandlers(() -> {
            var product = repo.findById(productId)
                    .orElseThrow(() -> new NoContentException("This product does not exist"));
            productUtils.checkUserIsSameUserForRequest(product.getCategory().getUser().getId(), req, "fetch");
            return product;
        });
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public Page<ProductModel> getAllProducts(Pageable pageable, HttpServletRequest req) {
        return exceptionHandlers(() -> {
            var userId = jwtUtils.getUserId(req.getHeader("refresh_token"));
            return repo.findAllByCategoryUserId(userId, pageable);
        });
    }

    /**
     * For Images update only: another users can't, update not owned products
     *
     * @param product   should files and id and images not to be null and user be null
     * @param productId should not to be null
     * @param req       should contain refresh token
     * @return updated product with new images
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public ProductModel updateProductImages(Optional<ProductModel> product, Long productId, HttpServletRequest req) {
        return exceptionHandlers(() -> {
            product.map(ProductModel::getId).ifPresent(id -> product.get().setId(null));
            var foundProduct = repo.findById(productId)
                    .orElseThrow(() -> new NoContentException("This product does not exist"));
            productUtils.checkUserIsSameUserForRequest(foundProduct.getCategory().getUser().getId(), req,
                    "update");
            try {
                return productUtils.updateProductImages(product, foundProduct);
            } catch (IOException e) {
                throw new InternalServerException(e.getLocalizedMessage());
            }
        });
    }

    /**
     * For Images delete only: another users can't update, not owned products
     *
     * @param product   should images name not to be null and user and id be null, image names are going to delete
     * @param productId should not to be null
     * @param req       should contain refresh token
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public ResponseEntity<?> updateDeleteProductImages(Optional<ProductModel> product, Long productId,
                                                       HttpServletRequest req) {
        return exceptionHandlers(() -> {
            product.map(ProductModel::getId).ifPresent(id -> product.get().setId(null));
            var foundProduct = repo.findById(productId)
                    .orElseThrow(() -> new NoContentException("This product does not exist"));
            productUtils.checkUserIsSameUserForRequest(foundProduct.getCategory().getUser().getId(), req,
                    "delete images of");
            product.orElseThrow(() -> new BadRequestException("Product can't be null"));
            productUtils.updateDeleteProductImages(product.get(), foundProduct);
            return new ResponseEntity<>(HttpStatus.OK);
        });
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_ADMIN','OP_ACCESS_USER')")
    public ResponseEntity<?> deleteProduct(Long id, HttpServletRequest req) {
        return exceptionHandlers(() -> {
            var product = repo.findById(id)
                    .orElseThrow(() -> new NoContentException("This product does not exist"));
            productUtils.checkUserIsSameUserForRequest(product.getCategory().getUser().getId(), req,
                    "delete");
            try {
                ioUtils.deleteProductFiles(product);
            } catch (IOException e) {
                throw new InternalServerException(e.getLocalizedMessage());
            }
            repo.deleteById(id);
            return ResponseEntity.ok("Deleted the product");
        });
    }

    private <T> T exceptionHandlers(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (DataException | BadRequestException e) {
            throw new BadRequestException(e.getLocalizedMessage());
        } catch (ForbiddenException e) {
            throw new ForbiddenException(e.getLocalizedMessage());
        } catch (NoContentException e) {
            throw new NoContentException(e.getLocalizedMessage());
        } catch (DataIntegrityViolationException e) {
            throw new DataExistsException("Product exists!");
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }
}


