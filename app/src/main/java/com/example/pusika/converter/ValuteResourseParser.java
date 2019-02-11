package com.example.pusika.converter;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class ValuteResourseParser {
    private ArrayList<Valute> valutes;

    public ValuteResourseParser() {
        valutes = new ArrayList<>();
    }

    public ArrayList<Valute> getValutes() {
        return valutes;
    }

    public boolean parse(XmlPullParser xpp) {
        boolean status = true;
        Valute currentValute = null;
        boolean inEntry = false;
        String textValue = "";

        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                String tagName = xpp.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("Valute".equalsIgnoreCase(tagName)) {
                            inEntry = true;
                            currentValute = new Valute();
                        }
                        break;
                    case XmlPullParser.TEXT:
                        textValue = xpp.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (inEntry) {
                            if ("Valute".equalsIgnoreCase(tagName)) {
                                valutes.add(currentValute);
                                inEntry = false;
                            } else if ("NumCode".equalsIgnoreCase(tagName)) {
                                currentValute.setNumCode(Integer.valueOf(textValue));
                            } else if ("CharCode".equalsIgnoreCase(tagName)) {
                                currentValute.setCharCode(textValue);
                            } else if ("Nominal".equalsIgnoreCase(tagName)) {
                                currentValute.setNominal(Integer.valueOf(textValue));
                            } else if ("Name".equalsIgnoreCase(tagName)) {
                                currentValute.setName(textValue);
                            } else if ("Value".equalsIgnoreCase(tagName)) {
                                currentValute.setValue(Double.valueOf(textValue.replace(",", ".")));
                            }
                        }
                        break;
                    default:
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            status = false;
            e.printStackTrace();
        }
        return status;
    }
}
