package ir.darkdeveloper.anbarinoo.controller.Financial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.darkdeveloper.anbarinoo.exception.BadRequestException;
import ir.darkdeveloper.anbarinoo.model.CategoryModel;
import ir.darkdeveloper.anbarinoo.model.Financial.BuyModel;
import ir.darkdeveloper.anbarinoo.model.Financial.FinancialModel;
import ir.darkdeveloper.anbarinoo.model.Financial.SellModel;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.service.CategoryService;
import ir.darkdeveloper.anbarinoo.service.Financial.BuyService;
import ir.darkdeveloper.anbarinoo.service.Financial.DebtOrDemandService;
import ir.darkdeveloper.anbarinoo.service.Financial.SellService;
import ir.darkdeveloper.anbarinoo.service.ProductService;
import ir.darkdeveloper.anbarinoo.service.UserService;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import ir.darkdeveloper.anbarinoo.util.UserUtils;
import netscape.javascript.JSObject;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public record FinancialControllerTest(UserService userService,
                                      ProductService productService,
                                      JwtUtils jwtUtils,
                                      WebApplicationContext webApplicationContext,
                                      CategoryService categoryService,
                                      BuyService buyService,
                                      SellService sellService,
                                      DebtOrDemandService dodService) {

    private static String refresh;
    private static String access;
    private static Long productId;
    private static Long buyId;
    private static Long sellId;
    private static Long catId;
    private static HttpServletRequest request;
    private static MockMvc mockMvc;
    private static LocalDateTime fromDate = null;
    private static LocalDateTime toDate = null;

    @Autowired
    public FinancialControllerTest {
    }

    @BeforeAll
    static void setUp() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        request = mock(HttpServletRequest.class);
    }

    @BeforeEach
    void setUp2() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @WithMockUser(username = "anonymousUser")
    void saveUser() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        var user = new UserModel();
        user.setEmail("email@mail.com");
        user.setAddress("address");
        user.setDescription("desc");
        user.setUserName("user n");
        user.setPassword("pass1");
        user.setPasswordRepeat("pass1");
        user.setEnabled(true);
        userService.signUpUser(user, response);
        request = setUpHeader(user.getEmail(), user.getId());
    }


    @Test
    @Order(2)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void saveCategory() {
        var electronics = new CategoryModel("Electronics");
        categoryService.saveCategory(electronics, request);
        catId = electronics.getId();
    }

    @Test
    @Order(3)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void saveProduct() {
        var product = new ProductModel();
        product.setName("name");
        product.setDescription("description");
        product.setTotalCount(BigDecimal.valueOf(50));
        product.setPrice(BigDecimal.valueOf(500));
        product.setCategory(new CategoryModel(catId));
        fromDate = LocalDateTime.now();
        productService.saveProduct(product, request);
        productId = product.getId();
    }

    @RepeatedTest(5)
    @Order(4)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void saveBuy() throws InterruptedException {
        var buy = new BuyModel();
        buy.setProduct(new ProductModel(productId));
        buy.setPrice(BigDecimal.valueOf(5000));
        buy.setCount(BigDecimal.valueOf(8));
        buyService.saveBuy(buy, false, request);
        assertThat(buy.getId()).isNotNull();
        buyId = buy.getId();
        Thread.sleep(1000);
    }

    @RepeatedTest(5)
    @Order(5)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void saveSell() throws InterruptedException {
        var sell = new SellModel();
        sell.setProduct(new ProductModel(productId));
        sell.setPrice(BigDecimal.valueOf(6000));
        sell.setCount(BigDecimal.valueOf(4));
        sellService.saveSell(sell, request);
        sellId = sell.getId();
        assertThat(sell.getId()).isNotNull();
        Thread.sleep(1000);
    }

    @Test
    @Order(6)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void badProductUpdate() {
        var product = new ProductModel();
        product.setTotalCount(BigDecimal.valueOf(9850));
        product.setPrice(BigDecimal.valueOf(564));
        assertThrows(BadRequestException.class, () -> productService.updateProduct(product, productId, request));
    }

    @Test
    @Order(7)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getProductsAfterSells() {
        var product = productService.getProduct(productId, request);
        assertThat(product.getTotalCount()).isEqualTo(BigDecimal.valueOf(700000, 4));
    }

    @Test
    @Order(8)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getCosts() throws Exception {
        var financial = new FinancialModel();
        financial.setFromDate(fromDate);
        toDate = LocalDateTime.now();
        financial.setToDate(toDate);

        var cost1 = BigDecimal.valueOf(50).multiply(BigDecimal.valueOf(500));
        var cost2 = BigDecimal.valueOf(5000).multiply(BigDecimal.valueOf(8));
        var tax1 = cost1.multiply(BigDecimal.valueOf(9, 2));
        var tax2 = cost2.multiply(BigDecimal.valueOf(9, 2));
        var finalCost1 = cost1.add(tax1);
        var finalCost2 = cost2.add(tax2).multiply(BigDecimal.valueOf(5));
        var finalCost = finalCost1.add(finalCost2).setScale(1, RoundingMode.CEILING);

        mockMvc.perform(post("/api/user/financial/costs/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("refresh_token", refresh)
                .header("access_token", access)
                .content(mapToJson(financial))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costs").value(is(finalCost), BigDecimal.class))
        ;
    }

    @Test
    @Order(9)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getIncomes() throws Exception {
        var financial = new FinancialModel();
        financial.setFromDate(fromDate);
        financial.setToDate(toDate);
        var income = BigDecimal.valueOf(6000).multiply(BigDecimal.valueOf(4));

        var tax = income.multiply(BigDecimal.valueOf(9, 2));

        var finalIncome = income.subtract(tax).multiply(BigDecimal.valueOf(5))
                .setScale(1, RoundingMode.CEILING);

        mockMvc.perform(post("/api/user/financial/incomes/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("refresh_token", refresh)
                .header("access_token", access)
                .content(mapToJson(financial))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomes").value(is(finalIncome), BigDecimal.class))
        ;
    }

    @Test
    @Order(10)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void updateABuyWithBiggerCountThanPrevious() {
        var buy = new BuyModel();
        buy.setPrice(BigDecimal.valueOf(6000));
        buy.setCount(BigDecimal.valueOf(20));
        buy.setProduct(new ProductModel(productId));
        buyService.updateBuy(buy, buyId, request);
    }

    @Test
    @Order(11)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void updateASellWithBiggerCountThanPrevious() {
        var sell = new SellModel();
        sell.setPrice(BigDecimal.valueOf(9000));
        sell.setCount(BigDecimal.valueOf(6));
        sell.setProduct(new ProductModel(productId));
        sellService.updateSell(sell, sellId, request);
    }

    @Test
    @Order(12)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void updateABuyWithLessCountThanPrevious() {
        var buy = new BuyModel();
        buy.setPrice(BigDecimal.valueOf(6000));
        buy.setCount(BigDecimal.valueOf(2));
        buy.setProduct(new ProductModel(productId));
        buyService.updateBuy(buy, buyId, request);
    }

    @Test
    @Order(13)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void updateASellWithLessCountThanPrevious() {
        var sell = new SellModel();
        sell.setPrice(BigDecimal.valueOf(9000));
        sell.setCount(BigDecimal.valueOf(3));
        sell.setProduct(new ProductModel(productId));
        sellService.updateSell(sell, sellId, request);
    }

    @Test
    @Order(14)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getProductsAfterSellAndBuyUpdate() {
        var product = productService.getProduct(productId, request);
        assertThat(product.getTotalCount()).isEqualTo(BigDecimal.valueOf(650000, 4));
    }

    @Test
    @Order(15)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getCostsAfterBuyAndSellUpdates() throws Exception {
        var financial = new FinancialModel();
        financial.setFromDate(fromDate);
        toDate = LocalDateTime.now();
        financial.setToDate(toDate);

        var cost1 = BigDecimal.valueOf(50).multiply(BigDecimal.valueOf(500));
        var cost2 = BigDecimal.valueOf(5000).multiply(BigDecimal.valueOf(8));
        var cost3 = BigDecimal.valueOf(6000).multiply(BigDecimal.valueOf(2));
        var tax1 = cost1.multiply(BigDecimal.valueOf(9, 2));
        var tax2 = cost2.multiply(BigDecimal.valueOf(9, 2));
        var tax3 = cost3.multiply(BigDecimal.valueOf(9, 2));
        var finalCost1 = cost1.add(tax1);
        var finalCost2 = cost2.add(tax2).multiply(BigDecimal.valueOf(4));
        var finalCost3 = cost3.add(tax3);
        var finalCost = finalCost1.add(finalCost2).add(finalCost3)
                .setScale(1, RoundingMode.CEILING);

        mockMvc.perform(post("/api/user/financial/costs/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("refresh_token", refresh)
                .header("access_token", access)
                .content(mapToJson(financial))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costs").value(is(finalCost), BigDecimal.class))
        ;
    }

    @Test
    @Order(16)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getIncomesAfterBuyAndSellUpdates() throws Exception {
        var financial = new FinancialModel();
        financial.setFromDate(fromDate);
        financial.setToDate(toDate);

        var income1 = BigDecimal.valueOf(6000).multiply(BigDecimal.valueOf(4));
        var income2 = BigDecimal.valueOf(9000).multiply(BigDecimal.valueOf(3));

        var tax1 = income1.multiply(BigDecimal.valueOf(9, 2));
        var tax2 = income2.multiply(BigDecimal.valueOf(9, 2));
        var finalIncome1 = income1.subtract(tax1).multiply(BigDecimal.valueOf(4));
        var finalIncome2 = income2.subtract(tax2);
        var finalIncome = finalIncome1.add(finalIncome2).setScale(1, RoundingMode.CEILING);

        mockMvc.perform(post("/api/user/financial/incomes/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("refresh_token", refresh)
                .header("access_token", access)
                .content(mapToJson(financial))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomes").value(is(finalIncome), BigDecimal.class))
        ;
    }


    @Test
    @Order(17)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getProfitAndLoss() throws Exception {
        var financial = new FinancialModel();
        financial.setFromDate(fromDate);
        financial.setToDate(toDate);

        var income1 = BigDecimal.valueOf(6000).multiply(BigDecimal.valueOf(4));
        var income2 = BigDecimal.valueOf(9000).multiply(BigDecimal.valueOf(3));

        var tax1 = income1.multiply(BigDecimal.valueOf(9, 2));
        var tax2 = income2.multiply(BigDecimal.valueOf(9, 2));
        var finalIncome1 = income1.subtract(tax1).multiply(BigDecimal.valueOf(4));
        var finalIncome2 = income2.subtract(tax2);
        var finalIncome = finalIncome1.add(finalIncome2).setScale(1, RoundingMode.CEILING);

        var cost1 = BigDecimal.valueOf(50).multiply(BigDecimal.valueOf(500));
        var cost2 = BigDecimal.valueOf(5000).multiply(BigDecimal.valueOf(8));
        var cost3 = BigDecimal.valueOf(6000).multiply(BigDecimal.valueOf(2));
        var tax1c = cost1.multiply(BigDecimal.valueOf(9, 2));
        var tax2c = cost2.multiply(BigDecimal.valueOf(9, 2));
        var tax3c = cost3.multiply(BigDecimal.valueOf(9, 2));
        var finalCost1 = cost1.add(tax1c);
        var finalCost2 = cost2.add(tax2c).multiply(BigDecimal.valueOf(4));
        var finalCost3 = cost3.add(tax3c);
        var finalCost = finalCost1.add(finalCost2).add(finalCost3)
                .setScale(1, RoundingMode.CEILING);

        var profitOrLoss = finalIncome.multiply(BigDecimal.valueOf(100)).divide(finalCost, 2, RoundingMode.CEILING);

        var finalProfitOrLoss = (BigDecimal) null;

        if (profitOrLoss.compareTo(BigDecimal.valueOf(100)) > 0)
            finalProfitOrLoss = profitOrLoss.subtract(BigDecimal.valueOf(100));
        else
            finalProfitOrLoss = BigDecimal.valueOf(100).subtract(profitOrLoss);


        var finalProfitOrLoss1 = finalProfitOrLoss;
        mockMvc.perform(post("/api/user/financial/profit-loss/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("refresh_token", refresh)
                .header("access_token", access)
                .content(mapToJson(financial))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var jObject = new JSONObject(result.getResponse().getContentAsString());
                    var fetchedProfitOrLoss = (BigDecimal) null;
                    if (jObject.get("loss")!= null)
                        fetchedProfitOrLoss = BigDecimal.valueOf(jObject.getDouble("loss"))
                                .setScale(2, RoundingMode.HALF_DOWN);

                    assertThat(fetchedProfitOrLoss).isEqualTo(finalProfitOrLoss1);
                })
        ;
    }


    private String mapToJson(Object obj) throws JsonProcessingException {
        return new ObjectMapper().findAndRegisterModules().writeValueAsString(obj);
    }

    //should return the object; data is being removed
    private HttpServletRequest setUpHeader(String email, Long userId) {

        Map<String, String> headers = new HashMap<>();
        headers.put(null, "HTTP/1.1 200 OK");
        headers.put("Content-Type", "text/html");

        refresh = jwtUtils.generateRefreshToken(email, userId);
        access = jwtUtils.generateAccessToken(email);
        var refreshDate = UserUtils.TOKEN_EXPIRATION_FORMAT.format(jwtUtils.getExpirationDate(refresh));
        var accessDate = UserUtils.TOKEN_EXPIRATION_FORMAT.format(jwtUtils.getExpirationDate(access));
        headers.put("refresh_token", refresh);
        headers.put("access_token", access);
        headers.put("refresh_expiration", refreshDate);
        headers.put("access_expiration", accessDate);


        HttpServletRequest request = mock(HttpServletRequest.class);
        for (String key : headers.keySet())
            when(request.getHeader(key)).thenReturn(headers.get(key));

        return request;
    }
}