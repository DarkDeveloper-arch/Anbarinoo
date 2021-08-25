package ir.darkdeveloper.anbarinoo.controller.Financial;

import ir.darkdeveloper.anbarinoo.model.Financial.SellModel;
import ir.darkdeveloper.anbarinoo.service.Financial.SellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/category/products/sell")
public class SellController {

    private final SellService service;

    @Autowired
    public SellController(SellService service) {
        this.service = service;
    }


    @PostMapping("/save/")
    public ResponseEntity<?> saveSell(@RequestBody SellModel sell, HttpServletRequest request) {
        return ResponseEntity.ok(service.saveSell(sell, request));
    }

    @PutMapping("/update/{id}/")
    public ResponseEntity<?> updateSell(@RequestBody SellModel sell, @PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok(service.updateSell(sell, id, request));
    }

    @GetMapping("/get-by-product/{id}/")
    public ResponseEntity<?> getAllSellRecordsOfProduct(@PathVariable("id") Long productId, HttpServletRequest request,
                                                        Pageable pageable) {
        return ResponseEntity.ok(service.getAllSellRecordsOfProduct(productId, request, pageable));
    }

    @GetMapping("/get-by-user/{id}/")
    public ResponseEntity<?> getAllSellRecordsOfUser(@PathVariable("id") Long userId, HttpServletRequest request,
                                                     Pageable pageable) {
        return ResponseEntity.ok(service.getAllSellRecordsOfUser(userId, request, pageable));
    }

    @GetMapping("/{id}/")
    public ResponseEntity<?> getSell(@PathVariable("id") Long sellId, HttpServletRequest request) {
        return ResponseEntity.ok(service.getSell(sellId, request));
    }

    @DeleteMapping("/{id}/")
    public ResponseEntity<?> deleteSell(@PathVariable("id") Long sellId, HttpServletRequest request) {
        service.deleteSell(sellId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
