package com.foodnest.common;

import com.foodnest.customer.Customer;
import com.foodnest.customer.CustomerRepository;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.dish.Dish;
import com.foodnest.dish.DishRepository;
import com.foodnest.restaurant.Restaurant;
import com.foodnest.restaurant.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CustomerRepository customerRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryManRepository deliveryManRepository;
    private final DishRepository dishRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CustomerRepository customerRepository,
                      RestaurantRepository restaurantRepository,
                      DeliveryManRepository deliveryManRepository,
                      DishRepository dishRepository,
                      PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryManRepository = deliveryManRepository;
        this.dishRepository = dishRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (restaurantRepository.count() == 0) {
            log.info("Seeding initial AdvanceFoodnest restaurants, dishes, customers, and delivery partners...");

            String defaultHash = passwordEncoder.encode("password123");

            // 1. Restaurant Fastfoods
            Restaurant fastfoods = new Restaurant();
            fastfoods.setName("Fastfoods");
            fastfoods.setOwnerName("Alice Smith");
            fastfoods.setEmail("fastfoods@foodnest.com");
            fastfoods.setPhone("9876500001");
            fastfoods.setPasswordHash(defaultHash);
            fastfoods.setAddress("123 MG Road, Brigade Junction, Bengaluru");
            fastfoods.setCuisineType("Burgers & Fast Food");
            fastfoods.setImageUrl("https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800");
            fastfoods.setRating(new BigDecimal("4.8"));
            fastfoods.setLatitude(12.9756);
            fastfoods.setLongitude(77.6066);
            fastfoods.setAccountStatus("ACTIVE");
            Restaurant savedFastfoods = restaurantRepository.save(fastfoods);

            createDish(savedFastfoods, "Chicken Fry", "Golden-crisp marinated spiced fried chicken pieces served with spicy dip.", "Fast Food", new BigDecimal("199.00"), 50, "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=600");
            createDish(savedFastfoods, "Chicken 65", "Authentic South Indian crispy spicy chicken starter tempered with curry leaves.", "Starters", new BigDecimal("220.00"), 40, "https://images.unsplash.com/photo-1610057099443-fde8c4d50f91?w=600");
            createDish(savedFastfoods, "Crispy Chicken Burger", "Juicy fried chicken patty with lettuce, tomatoes, melted cheese and special sauce.", "Burgers", new BigDecimal("159.00"), 60, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600");
            createDish(savedFastfoods, "Peri Peri French Fries", "Freshly fried potato strips tossed in fiery African peri peri seasoning.", "Sides", new BigDecimal("99.00"), 80, "https://images.unsplash.com/photo-1576107232684-1279f3908594?w=600");

            // 2. Restaurant Foodies
            Restaurant foodies = new Restaurant();
            foodies.setName("Foodies");
            foodies.setOwnerName("Bob Johnson");
            foodies.setEmail("foodies@foodnest.com");
            foodies.setPhone("9876500002");
            foodies.setPasswordHash(defaultHash);
            foodies.setAddress("45 Residency Road, Ashok Nagar, Bengaluru");
            foodies.setCuisineType("Biryani & Mughlai");
            foodies.setImageUrl("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800");
            foodies.setRating(new BigDecimal("4.9"));
            foodies.setLatitude(12.9716);
            foodies.setLongitude(77.6085);
            foodies.setAccountStatus("ACTIVE");
            Restaurant savedFoodies = restaurantRepository.save(foodies);

            createDish(savedFoodies, "Mutton Fry", "Tender young mutton slow-cooked with roasted spices, garlic, and cracked pepper.", "Starters", new BigDecimal("299.00"), 35, "https://images.unsplash.com/photo-1544025162-d76694265947?w=600");
            createDish(savedFoodies, "Hyderabadi Chicken Biryani", "Fragrant long-grain basmati rice layered with succulent spiced chicken and saffron.", "Biryani", new BigDecimal("280.00"), 75, "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=600");
            createDish(savedFoodies, "Royal Mutton Dum Biryani", "Classic slow-dum cooked goat mutton biryani with caramelized onions and whole spices.", "Biryani", new BigDecimal("380.00"), 45, "https://images.unsplash.com/photo-1633945274405-b6c8069047b0?w=600");
            createDish(savedFoodies, "Garlic Butter Naan", "Tandoor baked leavened flatbread brushed with garlic butter and fresh cilantro.", "Breads", new BigDecimal("50.00"), 120, "https://images.unsplash.com/photo-1626074353765-517a681e40be?w=600");

            // 3. Restaurant SpiceGarden
            Restaurant spiceGarden = new Restaurant();
            spiceGarden.setName("SpiceGarden");
            spiceGarden.setOwnerName("Charlie Patel");
            spiceGarden.setEmail("spicegarden@foodnest.com");
            spiceGarden.setPhone("9876500003");
            spiceGarden.setPasswordHash(defaultHash);
            spiceGarden.setAddress("88 Indiranagar 100ft Road, Bengaluru");
            spiceGarden.setCuisineType("South Indian & Pure Veg");
            spiceGarden.setImageUrl("https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800");
            spiceGarden.setRating(new BigDecimal("4.7"));
            spiceGarden.setLatitude(12.9784);
            spiceGarden.setLongitude(77.6408);
            spiceGarden.setAccountStatus("ACTIVE");
            Restaurant savedSpiceGarden = restaurantRepository.save(spiceGarden);

            createDish(savedSpiceGarden, "Ghee Masala Dosa", "Crispy golden crepe with potato masala filling, served with coconut chutney & sambar.", "South Indian", new BigDecimal("110.00"), 100, "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=600");
            createDish(savedSpiceGarden, "Paneer Butter Masala", "Cottage cheese cubes simmered in a rich, buttery tomato cream gravy.", "North Indian", new BigDecimal("240.00"), 50, "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=600");
            createDish(savedSpiceGarden, "Hot Sambar Vada (2 pcs)", "Crispy lentil fritters soaked in steaming aromatic lentil stew.", "South Indian", new BigDecimal("75.00"), 60, "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=600");
            createDish(savedSpiceGarden, "Gulab Jamun (2 pcs)", "Soft melt-in-mouth milk solids dumplings soaked in rose cardamom sugar syrup.", "Desserts", new BigDecimal("60.00"), 90, "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=600");

            // 4. Default Customers
            Customer c1 = new Customer();
            c1.setFullName("Rahul Sharma");
            c1.setEmail("customer@foodnest.com");
            c1.setPhone("9876543200");
            c1.setPasswordHash(defaultHash);
            c1.setAddress("Flat 402, Green Glen Layout, Bellandur, Bengaluru");
            customerRepository.save(c1);

            Customer c2 = new Customer();
            c2.setFullName("Ram Kumar");
            c2.setEmail("ram@foodnest.com");
            c2.setPhone("9876543201");
            c2.setPasswordHash(defaultHash);
            c2.setAddress("Villa 12, Palm Meadows, Whitefield, Bengaluru");
            customerRepository.save(c2);

            // 5. Default Delivery Partners (offline by default until they log in and toggle online)
            DeliveryMan d1 = new DeliveryMan();
            d1.setFullName("David Vance");
            d1.setEmail("rider1@foodnest.com");
            d1.setPhone("9876543210");
            d1.setPasswordHash(defaultHash);
            d1.setVehicleType("Honda Activa");
            d1.setIsAvailable(false);
            d1.setAccountStatus("ACTIVE");
            deliveryManRepository.save(d1);

            DeliveryMan d2 = new DeliveryMan();
            d2.setFullName("Elena Rostova");
            d2.setEmail("rider2@foodnest.com");
            d2.setPhone("9876543211");
            d2.setPasswordHash(defaultHash);
            d2.setVehicleType("Yamaha FZ");
            d2.setIsAvailable(false);
            d2.setAccountStatus("ACTIVE");
            deliveryManRepository.save(d2);

            log.info("AdvanceFoodnest seed data initialization completed successfully!");
        }
    }

    private void createDish(Restaurant restaurant, String name, String desc, String cat, BigDecimal price, int stock, String img) {
        Dish dish = new Dish();
        dish.setRestaurant(restaurant);
        dish.setName(name);
        dish.setDescription(desc);
        dish.setCategory(cat);
        dish.setPrice(price);
        dish.setAvailableStock(stock);
        dish.setImageUrl(img);
        dish.setIsAvailable(true);
        dishRepository.save(dish);
    }
}
