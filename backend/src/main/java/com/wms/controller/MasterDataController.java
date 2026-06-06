package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.common.BusinessCodeGenerator;
import com.wms.common.BusinessException;
import com.wms.entity.BaseEntity;
import com.wms.entity.Customer;
import com.wms.entity.Location;
import com.wms.entity.Owner;
import com.wms.entity.Product;
import com.wms.entity.Supplier;
import com.wms.entity.Warehouse;
import com.wms.repository.CustomerRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.OwnerRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.SupplierRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterDataController {

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final OwnerRepository ownerRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @GetMapping("/lookups")
    public ApiResponse<Map<String, Object>> lookups() {
        Map<String, Object> data = new HashMap<>();
        data.put("warehouses", warehouseRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        data.put("locations", locationRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        data.put("owners", ownerRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        data.put("suppliers", supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        data.put("customers", customerRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        data.put("products", productRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return ApiResponse.ok(data);
    }

    @GetMapping("/warehouses")
    public ApiResponse<?> warehouses() {
        return ApiResponse.ok(warehouseRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }

    @PostMapping("/warehouses")
    public ApiResponse<?> createWarehouse(@RequestBody Warehouse warehouse) {
        applyWarehouseDefaults(warehouse, null);
        return ApiResponse.ok(warehouseRepository.save(warehouse));
    }

    @PutMapping("/warehouses/{id}")
    public ApiResponse<?> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        Warehouse existing = warehouseRepository.findById(id).orElseThrow();
        warehouse.setId(id);
        preserveAuditFields(warehouse, existing);
        applyWarehouseDefaults(warehouse, existing);
        return ApiResponse.ok(warehouseRepository.save(warehouse));
    }

    @DeleteMapping("/warehouses/{id}")
    public ApiResponse<?> deleteWarehouse(@PathVariable Long id) {
        warehouseRepository.deleteById(id);
        return ApiResponse.ok("deleted", null);
    }

    @GetMapping("/locations")
    public ApiResponse<?> locations() {
        return ApiResponse.ok(locationRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }

    @PostMapping("/locations")
    public ApiResponse<?> createLocation(@RequestBody Location location) {
        if (location.getCapacityQty() == null) {
            location.setCapacityQty(BigDecimal.ZERO);
        }
        if (location.getUsedQty() == null) {
            location.setUsedQty(BigDecimal.ZERO);
        }
        if (location.getStatus() == null) {
            location.setStatus("ACTIVE");
        }
        if (location.getPickable() == null) {
            location.setPickable(Boolean.TRUE);
        }
        return ApiResponse.ok(locationRepository.save(location));
    }

    @PutMapping("/locations/{id}")
    public ApiResponse<?> updateLocation(@PathVariable Long id, @RequestBody Location location) {
        Location existing = locationRepository.findById(id).orElseThrow();
        location.setId(id);
        preserveAuditFields(location, existing);
        return ApiResponse.ok(locationRepository.save(location));
    }

    @DeleteMapping("/locations/{id}")
    public ApiResponse<?> deleteLocation(@PathVariable Long id) {
        locationRepository.deleteById(id);
        return ApiResponse.ok("deleted", null);
    }

    @GetMapping("/owners")
    public ApiResponse<?> owners() {
        return ApiResponse.ok(ownerRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }

    @PostMapping("/owners")
    public ApiResponse<?> createOwner(@RequestBody Owner owner) {
        if (owner.getOwnerType() == null) {
            owner.setOwnerType("SELF");
        }
        if (owner.getStatus() == null) {
            owner.setStatus("ACTIVE");
        }
        return ApiResponse.ok(ownerRepository.save(owner));
    }

    @PutMapping("/owners/{id}")
    public ApiResponse<?> updateOwner(@PathVariable Long id, @RequestBody Owner owner) {
        Owner existing = ownerRepository.findById(id).orElseThrow();
        owner.setId(id);
        preserveAuditFields(owner, existing);
        return ApiResponse.ok(ownerRepository.save(owner));
    }

    @DeleteMapping("/owners/{id}")
    public ApiResponse<?> deleteOwner(@PathVariable Long id) {
        ownerRepository.deleteById(id);
        return ApiResponse.ok("deleted", null);
    }

    @GetMapping("/suppliers")
    public ApiResponse<?> suppliers() {
        return ApiResponse.ok(supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }

    @PostMapping("/suppliers")
    public ApiResponse<?> createSupplier(@RequestBody Supplier supplier) {
        if (supplier.getStatus() == null) {
            supplier.setStatus("ACTIVE");
        }
        return ApiResponse.ok(supplierRepository.save(supplier));
    }

    @PutMapping("/suppliers/{id}")
    public ApiResponse<?> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        Supplier existing = supplierRepository.findById(id).orElseThrow();
        supplier.setId(id);
        preserveAuditFields(supplier, existing);
        return ApiResponse.ok(supplierRepository.save(supplier));
    }

    @DeleteMapping("/suppliers/{id}")
    public ApiResponse<?> deleteSupplier(@PathVariable Long id) {
        supplierRepository.deleteById(id);
        return ApiResponse.ok("deleted", null);
    }

    @GetMapping("/customers")
    public ApiResponse<?> customers() {
        return ApiResponse.ok(customerRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }

    @PostMapping("/customers")
    public ApiResponse<?> createCustomer(@RequestBody Customer customer) {
        if (customer.getCustomerType() == null) {
            customer.setCustomerType("B2B");
        }
        if (customer.getStatus() == null) {
            customer.setStatus("ACTIVE");
        }
        return ApiResponse.ok(customerRepository.save(customer));
    }

    @PutMapping("/customers/{id}")
    public ApiResponse<?> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        Customer existing = customerRepository.findById(id).orElseThrow();
        customer.setId(id);
        preserveAuditFields(customer, existing);
        return ApiResponse.ok(customerRepository.save(customer));
    }

    @DeleteMapping("/customers/{id}")
    public ApiResponse<?> deleteCustomer(@PathVariable Long id) {
        customerRepository.deleteById(id);
        return ApiResponse.ok("deleted", null);
    }

    @GetMapping("/products")
    public ApiResponse<?> products() {
        return ApiResponse.ok(productRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }

    @PostMapping("/products")
    public ApiResponse<?> createProduct(@RequestBody Product product) {
        applyProductDefaults(product, null);
        return ApiResponse.ok(productRepository.save(product));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<?> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        Product existing = productRepository.findById(id).orElseThrow();
        product.setId(id);
        preserveAuditFields(product, existing);
        applyProductDefaults(product, existing);
        return ApiResponse.ok(productRepository.save(product));
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse<?> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return ApiResponse.ok("deleted", null);
    }

    private void applyWarehouseDefaults(Warehouse warehouse, Warehouse existing) {
        if (warehouse.getWarehouseType() == null) {
            warehouse.setWarehouseType(existing == null ? "GENERAL" : existing.getWarehouseType());
        }
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(existing == null ? "ACTIVE" : existing.getStatus());
        }
        if (warehouse.getSceneType() == null) {
            warehouse.setSceneType(existing == null ? "GENERAL_STORAGE" : existing.getSceneType());
        }
        if (warehouse.getDwellAlertMinutes() == null) {
            warehouse.setDwellAlertMinutes(existing == null ? 0 : existing.getDwellAlertMinutes());
        }
        if (warehouse.getSmsNotifyEnabled() == null) {
            warehouse.setSmsNotifyEnabled(existing == null ? Boolean.FALSE : existing.getSmsNotifyEnabled());
        }
        if (warehouse.getSmsReminderIntervalMinutes() == null) {
            warehouse.setSmsReminderIntervalMinutes(existing == null ? 120 : existing.getSmsReminderIntervalMinutes());
        }
        if (warehouse.getAutoAssignLocation() == null) {
            warehouse.setAutoAssignLocation(existing == null ? Boolean.TRUE : existing.getAutoAssignLocation());
        }
        if (warehouse.getScanMode() == null) {
            warehouse.setScanMode(existing == null ? "QR_CODE" : existing.getScanMode());
        }
    }

    private void applyProductDefaults(Product product, Product existing) {
        if (product.getUnitName() == null) {
            product.setUnitName(existing == null ? "件" : existing.getUnitName());
        }
        if (product.getSafeStock() == null) {
            product.setSafeStock(existing == null ? BigDecimal.ZERO : existing.getSafeStock());
        }
        if (product.getMaxStock() == null) {
            product.setMaxStock(existing == null ? BigDecimal.ZERO : existing.getMaxStock());
        }
        if (product.getShelfLifeDays() == null) {
            product.setShelfLifeDays(existing == null ? 0 : existing.getShelfLifeDays());
        }
        if (product.getEnableBatch() == null) {
            product.setEnableBatch(existing == null ? Boolean.TRUE : existing.getEnableBatch());
        }
        if (product.getEnableSerial() == null) {
            product.setEnableSerial(existing == null ? Boolean.FALSE : existing.getEnableSerial());
        }
        if (product.getWeightKg() == null) {
            product.setWeightKg(existing == null ? BigDecimal.ZERO : existing.getWeightKg());
        }
        if (product.getVolumeM3() == null) {
            product.setVolumeM3(existing == null ? BigDecimal.ZERO : existing.getVolumeM3());
        }
        if (product.getSalePrice() == null) {
            product.setSalePrice(existing == null ? BigDecimal.ZERO : existing.getSalePrice());
        }
        if (product.getStatus() == null) {
            product.setStatus(existing == null ? "ACTIVE" : existing.getStatus());
        }
        product.setBarcode(resolveProductBarcode(product, existing));
    }

    private String resolveProductBarcode(Product product, Product existing) {
        String barcode = product.getBarcode();
        if (barcode == null || barcode.isBlank()) {
            barcode = existing != null && existing.getBarcode() != null && !existing.getBarcode().isBlank()
                    ? existing.getBarcode()
                    : generateUniqueProductBarcode();
        } else {
            barcode = barcode.trim();
        }

        String resolvedBarcode = barcode;
        productRepository.findFirstByBarcodeOrderByIdAsc(resolvedBarcode)
                .filter(matched -> product.getId() == null || !product.getId().equals(matched.getId()))
                .ifPresent(matched -> {
                    throw new BusinessException("条码已存在: " + resolvedBarcode);
                });
        return resolvedBarcode;
    }

    private String generateUniqueProductBarcode() {
        for (int i = 0; i < 20; i++) {
            String barcode = BusinessCodeGenerator.productBarcode();
            if (productRepository.findFirstByBarcodeOrderByIdAsc(barcode).isEmpty()) {
                return barcode;
            }
        }
        throw new BusinessException("自动生成商品条码失败，请重试");
    }

    private void preserveAuditFields(BaseEntity target, BaseEntity existing) {
        target.setCreatedAt(existing.getCreatedAt());
        target.setUpdatedAt(existing.getUpdatedAt());
    }
}
