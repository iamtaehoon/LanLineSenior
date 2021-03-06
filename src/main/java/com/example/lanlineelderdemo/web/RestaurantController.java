package com.example.lanlineelderdemo.web;

import com.example.lanlineelderdemo.domain.restaurant.FoodCategory;
import com.example.lanlineelderdemo.domain.restaurant.Location;
import com.example.lanlineelderdemo.restaurant.dto.service.*;
import com.example.lanlineelderdemo.web.form.review.ReviewCreateForm;
import com.example.lanlineelderdemo.utils.enums.EnumMapper;
import com.example.lanlineelderdemo.utils.enums.EnumValue;
import com.example.lanlineelderdemo.utils.ExcelFileManager;
import com.example.lanlineelderdemo.restaurant.dto.controller.ShowRestaurantDetailsResponseDto;
import com.example.lanlineelderdemo.domain.menu.OpenType;
import com.example.lanlineelderdemo.menu.MenuService;
import com.example.lanlineelderdemo.restaurant.RestaurantService;
import com.example.lanlineelderdemo.web.form.menu.MenuForm;
import com.example.lanlineelderdemo.review.ReviewService;
import com.example.lanlineelderdemo.web.form.restaurant.SearchForm;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class RestaurantController {
    private static final int MAX_RESTAURANT_CNT = 5;
    private static final int EACH_RESTAURANT_PARAM_CNT = 7;
    private final EnumMapper enumMapper;
    private final RestaurantService restaurantService;
    private final MenuService menuService;
    private final ReviewService reviewService;

    @ModelAttribute("locations")
    public Map<String, String> locations() {
        Map<String, List<EnumValue>> enums = enumMapper.get("locations");
        List<EnumValue> enumValues = enums.get("locations");

        Map<String, String> locations = new LinkedHashMap<>();
        for (EnumValue enumValue : enumValues) {
            locations.put(enumValue.getKey(), enumValue.getValue());
        }
        return locations;
    }

    @ModelAttribute("foodCategories")
    public Map<String, String> foodCategories() {
        Map<String, List<EnumValue>> enums = enumMapper.get("foodCategories");
        List<EnumValue> enumValues = enums.get("foodCategories");
        Map<String, String> foodCategories = new LinkedHashMap<>();
        for (EnumValue enumValue : enumValues) {
            foodCategories.put(enumValue.getKey(), enumValue.getValue());
        }
        return foodCategories;
    }

    @ModelAttribute("openTypes")
    public Map<String, String> openTypes() {
        Map<String, List<EnumValue>> enums = enumMapper.get("openTypes");
        List<EnumValue> enumValues = enums.get("openTypes");

        Map<String, String> foodCategories = new LinkedHashMap<>();
        for (EnumValue enumValue : enumValues) {
            if (enumValue.getKey() == OpenType.BOTH.getKey()) {
                continue;
            }
            foodCategories.put(enumValue.getKey(), enumValue.getValue());
        }
        return foodCategories;
    }

    /**
     * ?????? ?????????
     */
    @GetMapping("/")
    public String searchRestaurantsForm(@ModelAttribute SearchForm form) {
        return "restaurants/searchForm";
    }

    @GetMapping("search")
    public String redirectSearchRestaurantsForm() {
        return "redirect:/";
    }

    /**
     * ??????
     */
    @PostMapping("search")
    public String searchRestaurants(@Validated @ModelAttribute SearchForm searchForm, BindingResult bindingResult) throws UnsupportedEncodingException {
        if (bindingResult.hasErrors()) {
            return "restaurants/searchForm";
        }

        List<SearchRestaurantResponseDto> restaurantsDto = restaurantService.searchRestaurants(searchForm.toEntity());
        List<String> restaurantsParam = makeParameters(restaurantsDto);
        return "redirect:/result?restaurants=" + URLEncoder.encode(StringUtils.join(restaurantsParam, ','));
    }

    private List<String> makeParameters(List<SearchRestaurantResponseDto> restaurants) {
        return restaurants.stream().map(restaurant -> restaurant.convertParam()).collect(Collectors.toList());
    }

    @GetMapping("result")
    public String searchRestaurantResult(@RequestParam List<String> restaurants, Model model) {
        try {
            List<SearchRestaurantResponseDto> results = new ArrayList<>();
            if (results.size() > MAX_RESTAURANT_CNT) {
                throw new IllegalArgumentException("????????? ???????????? ???????????????.");
            }
            for (String restaurant : restaurants) {
                results.add(makeResponsePuttingValue(restaurant));
            }
            model.addAttribute("results", results);
            return "restaurants/resultPage";
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
    }

    private SearchRestaurantResponseDto makeResponsePuttingValue(String restaurant) {
        SearchRestaurantResponseDto restaurantResponseDto = new SearchRestaurantResponseDto();

        String[] infos = restaurant.split("%");
        if (infos.length != EACH_RESTAURANT_PARAM_CNT) {
            throw new IllegalArgumentException("????????? ???????????? ???????????????.");
        }
        for (String info : infos) {
            String[] idAndValue = info.split("=");
            if (idAndValue.length != 2) {
                throw new IllegalArgumentException("????????? ???????????? ???????????????.");
            }
            putValueUsingId(restaurantResponseDto,idAndValue);
        }
        return restaurantResponseDto;
    }

    private SearchRestaurantResponseDto putValueUsingId(SearchRestaurantResponseDto restaurantResponseDto, String[] idAndValue) {
        String id = idAndValue[0];
        String value = idAndValue[1];

        if (id.equals("id")) {
            restaurantResponseDto.setId(Long.valueOf(value));
        }
        else if (id.equals("name")) {
            restaurantResponseDto.setName(value);
        }
        else if (id.equals("location")) {
            restaurantResponseDto.setLocation(Location.find(value));
        }
        else if (id.equals("category")) {
            restaurantResponseDto.setCategory(FoodCategory.find(value));
        }
        else if (id.equals("locationX")) {
            restaurantResponseDto.setLocationX(Double.valueOf(value));
        }
        else if (id.equals("locationY")) {
            restaurantResponseDto.setLocationY(Double.valueOf(value));
        } else if (id.equals("minCostPerPerson")) {
            restaurantResponseDto.setMinCostPerPerson(Integer.valueOf(value));
        } else {
            throw new IllegalArgumentException("????????? ???????????? ???????????????.");
        }
        return restaurantResponseDto;
    }

    /**
     * ????????????
     */
    @GetMapping("/restaurants/{restaurantId}")
    public String showRestaurantDetails(@PathVariable Long restaurantId, Model model,
                                        @ModelAttribute ReviewCreateForm reviewCreateForm) {
        try {
            model.addAttribute("restaurant", makeRestaurantDetailInfo(restaurantId));
            model.addAttribute("reviews", reviewService.inqueryRestaurantReviews(restaurantId));

            model.addAttribute("reviewCreateForm", reviewCreateForm);
            return "restaurants/detailPage";
        } catch (Exception e) {
            return "errorPage";
        }
    }

    private ShowRestaurantDetailsResponseDto makeRestaurantDetailInfo(Long restaurantId) {
        RestaurantResponseDto inqueryRestaurantResponse = restaurantService.inqueryRestaurant(restaurantId);
        RestaurantRecommendMenuDto recommendMenu = menuService.findRestaurantRecommendMenu(restaurantId);
        ShowRestaurantDetailsResponseDto restaurant = ShowRestaurantDetailsResponseDto.create(inqueryRestaurantResponse, recommendMenu);
        return restaurant;
    }

    /**
     * ?????? ????????? GetMapping (Admin???) ?????? ???????????? ?????? ?????????????????? ????????????. ?????? ?????? ????????????.
     */
    @GetMapping("/restaurants/new")
    public String registerRestaurantForm(@ModelAttribute MultipartFile file) {
        return "restaurants/registerForm";
    }

    /**
     * ?????? (Admin???)
     * 201 created Ok / ????????? ?????? ?????? ?????? ????????????
     * ??????????????? ???????????? ?????? http ???????????? ??? ????????? ?????????.
     */
    @PostMapping("/restaurants")
    public String registerRestaurantByAdmin(@ModelAttribute MultipartFile file) throws IOException{
        Sheet worksheet = ExcelFileManager.validateExcelFileIsAvailable(file);

        List<RestaurantCreateServiceRequestDto> dataList = new ArrayList<>();
        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            System.out.println("iNumber: "+i);
            Row row = worksheet.getRow(i);
            dataList.add(new RestaurantCreateServiceRequestDto(row));
        }
        restaurantService.registerRestaurants(dataList);
        return "redirect:/";
    }

    /**
     * ?????? ?????? ????????? GetMapping (Admin???) ?????? ???????????? ?????? ?????????????????? ????????????. ?????? ?????? ????????????.
     */
    @GetMapping("/menu/new")
    public String registerMenuForm(@ModelAttribute MultipartFile file) {
        return "registerMenuForm";
    }

    /**
     * ?????? ?????? (Admin???)
     */
    @PostMapping("/menu")
    public String registerMenus(@ModelAttribute MultipartFile file) throws IOException{
        List<MenuForm> dataList = new ArrayList<>();
        Sheet worksheet = ExcelFileManager.validateExcelFileIsAvailable(file);
        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) { // 4
            Row row = worksheet.getRow(i);
            dataList.add(new MenuForm(row));
            //TODO ?????? ?????? ????????? ????????? ????????? ???????????????.
            // ?????? ?????? ??????????????? ?????? ???????????? menuService.registerMenu??? ??????
        }
        menuService.registerMenus(dataList);
        return "redirect:/";
    }

    /**
     * ??????
     * ?????? http ??????????????? / ????????? ????????? ???????
     */



    @GetMapping("/noResult")
    public String notFound() {
        return "errorPage";
    }
}
