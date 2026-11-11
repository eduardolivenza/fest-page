package com.festdescans.api.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.application.port.out.AdminUserRepository;
import com.festdescans.api.application.port.out.ContactRepository;
import com.festdescans.api.application.port.out.CustomerRepository;
import com.festdescans.api.application.port.out.IdGenerator;
import com.festdescans.api.application.port.out.OrderRepository;
import com.festdescans.api.application.port.out.ProductRepository;
import com.festdescans.api.application.port.out.TransactionRunner;
import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.order.Customer;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderItem;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.order.ProductSummary;
import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductCategory;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.product.ProductImage;
import com.festdescans.api.domain.product.ProductInUseException;
import com.festdescans.api.domain.product.ProductSize;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = PersistenceAdaptersTest.App.class)
class PersistenceAdaptersTest {

    private static final EmbeddedPostgresDatabase DB = EmbeddedPostgresDatabase.create();
    private static final Instant T0 = Instant.parse("2026-11-01T10:00:00.123Z");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::jdbcUrl);
        registry.add("spring.datasource.username", DB::username);
        registry.add("spring.datasource.password", DB::password);
    }

    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired OrderRepository orders;
    @Autowired ContactRepository contacts;
    @Autowired AdminUserRepository admins;
    @Autowired TransactionRunner tx;
    @Autowired IdGenerator ids;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void emptyTables() {
        jdbc.sql("""
                truncate "OrderItem", "Order", "Customer", "ProductSize", "ProductImage", "Product",
                         "ContactRequest", "AdminUser" cascade""").update();
    }

    private Product product(String id, ProductCategory category, boolean featured, Instant createdAt,
            List<ProductSize> sizes) {
        return new Product(id, "Product " + id, "slug-" + id, "Desc", new BigDecimal("499.90"), category, featured,
                List.of(), sizes, createdAt, createdAt);
    }

    @Test
    void storesAndLoadsAProductWithEnumsDecimalsAndUtcTimestamps() {
        products.save(product("p1", ProductCategory.MATTRESS, true, T0,
                List.of(new ProductSize("s2", 150, 190, new BigDecimal("599.90"), "p1"),
                        new ProductSize("s1", 90, 190, new BigDecimal("549.90"), "p1"))));
        jdbc.sql("insert into \"ProductImage\" (id, url, alt, position, \"productId\") values ('i2','/b.jpg',null,2,'p1'),('i1','/a.jpg','A',1,'p1')")
                .update();

        Product loaded = products.findById("p1").orElseThrow();

        assertThat(loaded.category()).isEqualTo(ProductCategory.MATTRESS);
        assertThat(loaded.price()).isEqualByComparingTo("499.90");
        assertThat(loaded.createdAt()).isEqualTo(T0);
        assertThat(loaded.sizes()).extracting(ProductSize::id).containsExactly("s1", "s2");
        assertThat(loaded.images()).extracting(ProductImage::id).containsExactly("i1", "i2");
        // Stored as UTC wall time in a timestamp-without-time-zone column, like Prisma did.
        assertThat(jdbc.sql("select to_char(\"createdAt\", 'YYYY-MM-DD HH24:MI:SS.MS') from \"Product\"")
                .query(String.class).single()).isEqualTo("2026-11-01 10:00:00.123");
        assertThat(products.findByIdOrSlug("slug-p1")).isPresent();
        assertThat(products.findByIdOrSlug("p1")).isPresent();
        assertThat(products.findBySlug("nope")).isEmpty();
    }

    @Test
    void updatingReplacesSizesAndKeepsImages() {
        Product p = products.save(product("p1", ProductCategory.BED, false, T0,
                List.of(new ProductSize("s1", 90, 190, BigDecimal.TEN, "p1"))));
        jdbc.sql("insert into \"ProductImage\" (id, url, position, \"productId\") values ('i1','/a.jpg',0,'p1')").update();
        Product withImage = products.findById("p1").orElseThrow();

        Product updated = products.save(new Product(p.id(), "Nou nom", p.slug(), null, new BigDecimal("12.50"),
                ProductCategory.PILLOW, true, withImage.images(),
                List.of(new ProductSize("s9", 135, 200, BigDecimal.ONE, "p1")), p.createdAt(), T0.plusSeconds(5)));

        assertThat(updated.name()).isEqualTo("Nou nom");
        assertThat(updated.images()).extracting(ProductImage::id).containsExactly("i1");
        assertThat(updated.sizes()).extracting(ProductSize::id).containsExactly("s9");
        assertThat(jdbc.sql("select count(*) from \"ProductSize\"").query(Integer.class).single()).isEqualTo(1);
        assertThat(products.findById("p1").orElseThrow().updatedAt()).isEqualTo(T0.plusSeconds(5));
    }

    @Test
    void cataloguePagesAreFilteredAndOrderedFeaturedFirstThenNewest() {
        products.save(product("old", ProductCategory.BED, false, T0, List.of()));
        products.save(product("new", ProductCategory.BED, false, T0.plusSeconds(10), List.of()));
        products.save(product("feat", ProductCategory.BED, true, T0.minusSeconds(10), List.of()));
        products.save(product("pillow", ProductCategory.PILLOW, false, T0.plusSeconds(20), List.of()));

        Page<Product> beds = products.findCatalogPage(new ProductFilter(ProductCategory.BED, false), new PageRequest(1, 2));
        Page<Product> secondPage = products.findCatalogPage(new ProductFilter(ProductCategory.BED, false),
                new PageRequest(2, 2));
        Page<Product> featured = products.findCatalogPage(new ProductFilter(null, true), new PageRequest(1, 10));

        assertThat(beds.data()).extracting(Product::id).containsExactly("feat", "new");
        assertThat(beds.total()).isEqualTo(3);
        assertThat(beds.totalPages()).isEqualTo(2);
        assertThat(secondPage.data()).extracting(Product::id).containsExactly("old");
        assertThat(secondPage.page()).isEqualTo(2);
        assertThat(featured.data()).extracting(Product::id).containsExactly("feat");
        assertThat(products.findPage(new PageRequest(1, 10)).data()).extracting(Product::id)
                .containsExactly("pillow", "new", "old", "feat");
    }

    @Test
    void ordersReferenceCustomersAndProductsAndBlockProductDeletion() {
        products.save(product("p1", ProductCategory.BED, false, T0, List.of()));
        products.save(product("p2", ProductCategory.BED, false, T0, List.of()));
        Customer customer = customers.save(new Customer("cu1", "Joan", "joan@example.com", null, T0, T0));
        ProductSummary p1 = new ProductSummary("p1", "Product p1", new BigDecimal("499.90"));

        Order saved = orders.save(new Order("o1", customer,
                List.of(new OrderItem("i1", "o1", p1, 2, new BigDecimal("450.00"))), OrderStatus.PENDING, "Matí", T0, T0));

        assertThat(saved.customer().email()).isEqualTo("joan@example.com");
        assertThat(saved.items()).singleElement().satisfies(item -> {
            assertThat(item.product().name()).isEqualTo("Product p1");
            assertThat(item.unitPrice()).isEqualByComparingTo("450.00");
        });

        orders.save(saved.changeStatus(OrderStatus.CONFIRMED, T0.plusSeconds(60)));
        assertThat(orders.findPage(OrderStatus.CONFIRMED, new PageRequest(1, 20)).data()).extracting(Order::id)
                .containsExactly("o1");
        assertThat(orders.findPage(OrderStatus.PENDING, new PageRequest(1, 20)).total()).isZero();
        assertThat(orders.findPage(null, new PageRequest(1, 20)).total()).isEqualTo(1);

        assertThatThrownBy(() -> products.deleteById("p1")).isInstanceOf(ProductInUseException.class);
        assertThat(products.deleteById("p2")).isTrue();
        assertThat(products.deleteById("p2")).isFalse();
    }

    @Test
    void customersAreFoundByEmailAndUpdatedInPlace() {
        customers.save(new Customer("cu1", "Joan", "joan@example.com", null, T0, T0));
        customers.save(new Customer("cu1", "Joan B", "joan@example.com", "600", T0, T0.plusSeconds(1)));

        Customer found = customers.findByEmail("joan@example.com").orElseThrow();
        assertThat(found.name()).isEqualTo("Joan B");
        assertThat(found.phone()).isEqualTo("600");
        assertThat(found.createdAt()).isEqualTo(T0);
    }

    @Test
    void contactsAreFilteredByReadAndDeleted() {
        contacts.save(new ContactRequest("c1", "Maria", "m@example.com", null, "Hola, voldria info", false, T0));
        contacts.save(new ContactRequest("c2", "Pere", "p@example.com", "600", "Hola, voldria info", true,
                T0.plusSeconds(1)));

        assertThat(contacts.findPage(null, new PageRequest(1, 20)).data()).extracting(ContactRequest::id)
                .containsExactly("c2", "c1");
        assertThat(contacts.findPage(false, new PageRequest(1, 20)).data()).extracting(ContactRequest::id)
                .containsExactly("c1");

        contacts.save(contacts.findById("c1").orElseThrow().markRead());
        assertThat(contacts.findById("c1").orElseThrow().read()).isTrue();
        assertThat(contacts.deleteById("c1")).isTrue();
        assertThat(contacts.deleteById("c1")).isFalse();
    }

    @Test
    void readsAdminUsers() {
        jdbc.sql("""
                insert into "AdminUser" (id, email, "passwordHash", name, "updatedAt")
                values ('a1', 'admin@festdescans.com', '$argon2id$v=19$x', 'Admin', now())""").update();

        assertThat(admins.findByEmail("admin@festdescans.com")).get().satisfies(a -> {
            assertThat(a.id()).isEqualTo("a1");
            assertThat(a.passwordHash()).startsWith("$argon2id$");
        });
        assertThat(admins.findById("a1")).isPresent();
        assertThat(admins.findByEmail("other@festdescans.com")).isEmpty();
    }

    @Test
    void aFailingTransactionWritesNothing() {
        assertThatThrownBy(() -> tx.inTransaction(() -> {
            customers.save(new Customer("cu1", "Joan", "joan@example.com", null, T0, T0));
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        assertThat(customers.findByEmail("joan@example.com")).isEmpty();
    }

    @Test
    void generatesUniqueCuids() {
        Set<String> generated = new HashSet<>();
        IntStream.range(0, 10_000).forEach(i -> generated.add(ids.newId()));

        assertThat(generated).hasSize(10_000).allMatch(id -> id.matches("^c[0-9a-z]{24}$"));
    }

    /** Only the persistence adapter. Deliberately not a @Configuration: never component-scanned. */
    @EnableAutoConfiguration
    @Import(PersistenceConfiguration.class)
    static class App {
    }
}
