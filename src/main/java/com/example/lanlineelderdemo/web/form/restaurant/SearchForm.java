package com.example.lanlineelderdemo.web.form.restaurant;

import com.example.lanlineelderdemo.domain.restaurant.FoodCategory;
import com.example.lanlineelderdemo.domain.restaurant.Location;
import com.example.lanlineelderdemo.domain.menu.OpenType;
import com.example.lanlineelderdemo.domain.SearchCondition;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ToString
public class SearchForm {

    private List<Location> locations;

    private List<FoodCategory> unselectedCategories;

    private Boolean isAtmosphere;

    private Boolean hasCostPerformance;

    private Boolean canEatSingle;

    @Range(min = 2500, max = 100000)
    private Integer maxCostLine;

    @NotNull(message="{essentialValue.notnull}")
    private OpenType openType;

    public SearchCondition toEntity() {
        return new SearchCondition(locations, unselectedCategories, isAtmosphere,
                hasCostPerformance, canEatSingle, maxCostLine, openType);


    }
}
