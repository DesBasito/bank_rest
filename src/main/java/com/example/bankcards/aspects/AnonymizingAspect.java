package com.example.bankcards.aspects;

import com.example.bankcards.entity.Card;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AnonymizingAspect {


  @AfterReturning(
      pointcut = "execution(com.example.bankcards.entity.Card com.example.bankcards.repository.CardRepository.*(..))",
      returning = "card")
  public void anonymizeCard(Card card) {
    if (card != null) {
      card.setCardNumber(card.getCardNumber().replaceAll(("\\d(?=.*\\d{4})"), "*"));
    }
  }
}
