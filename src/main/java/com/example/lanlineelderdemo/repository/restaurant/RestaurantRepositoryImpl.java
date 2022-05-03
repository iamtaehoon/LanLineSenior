package com.example.lanlineelderdemo.repository.restaurant;

import com.example.lanlineelderdemo.domain.*;
import com.example.lanlineelderdemo.domain.menu.OpenType;
import com.example.lanlineelderdemo.domain.restaurant.Location;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import javax.persistence.EntityManager;
import java.util.List;

import static com.example.lanlineelderdemo.domain.menu.QMenu.menu;
import static com.example.lanlineelderdemo.domain.restaurant.QRestaurant.restaurant;

public class RestaurantRepositoryImpl implements RestaurantRepositoryCustom {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public RestaurantRepositoryImpl(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<FindRestaurantBySearchConditionResponseDto> findRestaurantBySearchCondition(SearchCondition searchCondition) {

        return queryFactory.select(Projections.bean(FindRestaurantBySearchConditionResponseDto.class, restaurant.id, restaurant.name, restaurant.location, restaurant.geoLocation,
                        restaurant.category, restaurant.isAtmosphere, restaurant.hasCostPerformance,
                        restaurant.canEatSingle, restaurant.adminComment, restaurant.url, menu.openType,
                        menu.menuName, menu.numberOfMeal, menu.price))
                .from(restaurant, menu)
                .join(menu.restaurant, restaurant)
                .where(includeLocations(searchCondition.getLocations()),
                        restaurant.category.notIn(searchCondition.getUnselectedCategories()),
                        eqIsAtmosphere(searchCondition.getIsAtmosphere()),
                        eqCanEatSingle(searchCondition.getCanEatSingle()),
                        eqHasCostPerformance(searchCondition.getHasCostPerformance()),
                        menu.openType.in(searchCondition.getOpenType(), OpenType.BOTH),
                        menu.price.divide(menu.numberOfMeal).loe(searchCondition.getMaxCostLine()))
                .orderBy(NumberExpression.random().asc())
                .limit(5)
                .fetch();
    }

    private BooleanExpression includeLocations(List<Location> locations) {
        if (locations.isEmpty()) {
            return null;
        }
        return restaurant.location.in(locations);
    }


    private BooleanExpression eqHasCostPerformance(Boolean costPerformance) {
        if (costPerformance == null) {
            return null;
        }
        return restaurant.isAtmosphere.eq(costPerformance);
    }

    private BooleanExpression eqCanEatSingle(Boolean canEatSingle) {
        if (canEatSingle == null) {
            return null;
        }
        return restaurant.isAtmosphere.eq(canEatSingle);
    }

    private BooleanExpression eqIsAtmosphere(Boolean isAtmosphere) {
        if (isAtmosphere == null) {
            return null;
        }
        return restaurant.isAtmosphere.eq(isAtmosphere);
    }
}