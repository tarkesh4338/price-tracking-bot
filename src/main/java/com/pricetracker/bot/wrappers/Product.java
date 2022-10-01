package com.pricetracker.bot.wrappers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Product {

  private String productName;
  private String brandName;
  private Double price;

}
