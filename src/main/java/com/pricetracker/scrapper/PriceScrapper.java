package com.pricetracker.scrapper;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pricetracker.bot.wrappers.Product;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriceScrapper {

  private static Logger log = LoggerFactory.getLogger(PriceScrapper.class);

  public static void main(String[] args) throws IOException {

    String url = "https://www.myntra.com/hair-cream-and-mask/loreal-professionnel/loreal-professionnel-xtenso-care-mask-196g/1444723/buy";
     String url1 = "https://dl.flipkart.com/s/ywCJ!ENNNN";
    
    Product product = getMyntraProductDetail(url);
    log.info("Myntra Product - {}", product);    
    
    Product product1 = getFlipkartProductDetail(url1);
    log.info("FlipKart Product - {}", product1);

  }

  public static Product getMyntraProductDetail(String url) throws IOException {
    log.info("Getting Item Details from Myntra for url : {}", url);
    Document doc = Jsoup.connect(url).timeout(5000).get();

    Elements elements = doc.getElementsByTag("script");
    for (Element s : elements) {
      if (s.toString().contains("pdpData")) {
        String data = StringUtils.substringAfter(s.data(), "window.__myx =");
        Product product = parseJsonGetProduct(data);
        return product;
      }
    }

    return null;
  }

  public static Product getFlipkartProductDetail(String url) throws IOException {
    log.info("Getting Item Details from Fliptkart for url : {}", url);
    Document doc = Jsoup.connect(url).timeout(5000).get();
    //  Class - aMaAEs
    Elements elements = doc.selectXpath("//*[@id=\"container\"]/div/div[3]/div[1]/div[2]/div[2]/div");
    Element e = elements.get(0);
    Elements bn = e.getElementsByClass("G6XhRU");
    String brandName = bn.size() > 0 ? bn.get(0).text() : null;
    Element productName = e.getElementsByClass("B_NuCI").get(0);
    Element price = e.getElementsByClass("_30jeq3 _16Jk6d").get(0);
    String productPrice = StringUtils.substringAfter(price.text().replace(",", ""), "₹");
    return new Product(productName.text(), brandName, Double.valueOf(productPrice));

  }

  private static Product parseJsonGetProduct(String data) {

    JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();

    JsonObject pdpData = jsonObject.get("pdpData").getAsJsonObject();
    String brandName = pdpData.get("brand").getAsJsonObject().get("name").getAsString();
    String productName = pdpData.get("name").getAsString();
    Double price = pdpData.get("price").getAsJsonObject().get("discounted").getAsDouble();

    return new Product(productName, brandName, price);
  }

}
