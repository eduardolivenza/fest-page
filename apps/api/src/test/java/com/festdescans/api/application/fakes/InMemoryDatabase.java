package com.festdescans.api.application.fakes;

import com.festdescans.api.application.port.out.AdminUserRepository;
import com.festdescans.api.application.port.out.ContactRepository;
import com.festdescans.api.application.port.out.CustomerRepository;
import com.festdescans.api.application.port.out.OrderRepository;
import com.festdescans.api.application.port.out.ProductRepository;
import com.festdescans.api.domain.admin.AdminUser;
import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.order.Customer;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.product.ProductInUseException;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * In-memory stand-in for PostgreSQL implementing every repository port, with the same ordering and
 * foreign-key rules as the real adapter. Tests seed it directly through the public maps.
 */
public final class InMemoryDatabase {

    public final Map<String, Product> products = new LinkedHashMap<>();
    public final Map<String, Customer> customers = new LinkedHashMap<>();
    public final Map<String, Order> orders = new LinkedHashMap<>();
    public final Map<String, ContactRequest> contacts = new LinkedHashMap<>();
    public final Map<String, AdminUser> admins = new LinkedHashMap<>();

    public final ProductRepository productRepository = new Products();
    public final CustomerRepository customerRepository = new Customers();
    public final OrderRepository orderRepository = new Orders();
    public final ContactRepository contactRepository = new Contacts();
    public final AdminUserRepository adminUserRepository = new Admins();

    public void clear() {
        products.clear();
        customers.clear();
        orders.clear();
        contacts.clear();
        admins.clear();
    }

    private static <T> Page<T> page(Collection<T> rows, Predicate<T> filter, Comparator<T> order, PageRequest page) {
        List<T> matching = rows.stream().filter(filter).sorted(order).toList();
        List<T> slice = matching.stream().skip(page.offset()).limit(page.pageSize()).toList();
        return new Page<>(slice, matching.size(), page.page(), page.pageSize());
    }

    private final class Products implements ProductRepository {

        private static final Comparator<Product> NEWEST = Comparator.comparing(Product::createdAt).reversed();

        @Override
        public Page<Product> findCatalogPage(ProductFilter filter, PageRequest page) {
            Predicate<Product> matches = p -> (filter.category() == null || p.category() == filter.category())
                    && (!filter.featuredOnly() || p.featured());
            Comparator<Product> order = Comparator.comparing(Product::featured).reversed().thenComparing(NEWEST);
            return page(products.values(), matches, order, page);
        }

        @Override
        public Page<Product> findPage(PageRequest page) {
            return page(products.values(), p -> true, NEWEST, page);
        }

        @Override
        public Optional<Product> findById(String id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public Optional<Product> findByIdOrSlug(String idOrSlug) {
            return products.values().stream()
                    .filter(p -> p.id().equals(idOrSlug) || p.slug().equals(idOrSlug))
                    .findFirst();
        }

        @Override
        public Optional<Product> findBySlug(String slug) {
            return products.values().stream().filter(p -> p.slug().equals(slug)).findFirst();
        }

        @Override
        public List<Product> findAllById(Collection<String> ids) {
            return products.values().stream().filter(p -> ids.contains(p.id())).toList();
        }

        @Override
        public Product save(Product product) {
            products.put(product.id(), product);
            return product;
        }

        @Override
        public boolean deleteById(String id) {
            if (!products.containsKey(id)) {
                return false;
            }
            boolean ordered = orders.values().stream()
                    .flatMap(o -> o.items().stream())
                    .anyMatch(i -> i.product().id().equals(id));
            if (ordered) {
                throw new ProductInUseException(id);
            }
            products.remove(id);
            return true;
        }
    }

    private final class Customers implements CustomerRepository {

        @Override
        public Optional<Customer> findByEmail(String email) {
            return customers.values().stream().filter(c -> c.email().equals(email)).findFirst();
        }

        @Override
        public Customer save(Customer customer) {
            customers.put(customer.id(), customer);
            return customer;
        }
    }

    private final class Orders implements OrderRepository {

        @Override
        public Page<Order> findPage(OrderStatus status, PageRequest page) {
            return page(orders.values(), o -> status == null || o.status() == status,
                    Comparator.comparing(Order::createdAt).reversed(), page);
        }

        @Override
        public Optional<Order> findById(String id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public Order save(Order order) {
            Objects.requireNonNull(customers.get(order.customer().id()), "customer must exist (foreign key)");
            orders.put(order.id(), order);
            return order;
        }
    }

    private final class Contacts implements ContactRepository {

        @Override
        public Page<ContactRequest> findPage(Boolean read, PageRequest page) {
            return page(contacts.values(), c -> read == null || c.read() == read,
                    Comparator.comparing(ContactRequest::createdAt).reversed(), page);
        }

        @Override
        public Optional<ContactRequest> findById(String id) {
            return Optional.ofNullable(contacts.get(id));
        }

        @Override
        public ContactRequest save(ContactRequest contact) {
            contacts.put(contact.id(), contact);
            return contact;
        }

        @Override
        public boolean deleteById(String id) {
            return contacts.remove(id) != null;
        }
    }

    private final class Admins implements AdminUserRepository {

        @Override
        public Optional<AdminUser> findByEmail(String email) {
            return admins.values().stream().filter(a -> a.email().equals(email)).findFirst();
        }

        @Override
        public Optional<AdminUser> findById(String id) {
            return Optional.ofNullable(admins.get(id));
        }
    }
}
