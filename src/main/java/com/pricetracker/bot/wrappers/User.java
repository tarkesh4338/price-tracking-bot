package com.pricetracker.bot.wrappers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {

  private Long userId;
  private String firstName;
  private String lastName;
  private String userName;
  private Long chatId;

}
