
package com.jobeso.RNWhatsAppStickers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeArray;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RNWhatsAppStickersModule extends ReactContextBaseJavaModule {
  public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
  public static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
  public static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

  public static final int ADD_PACK = 200;
  public static final String ERROR_ADDING_STICKER_PACK = "Could not add this sticker pack. Please install the latest version of WhatsApp before adding sticker pack";

  private final ReactApplicationContext reactContext;

  public RNWhatsAppStickersModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  //TODO: Convert return type into Readable

  @Override
  public String getName() {
    return "RNWhatsAppStickers";
  }

  @ReactMethod
  public void test(Promise promise){
    promise.resolve("");
  }

  public static String getContentProviderAuthority(Context context){
    return context.getPackageName() + ".stickercontentprovider";
  }

  @ReactMethod
  public void send(String identifier, String stickerPackName, Promise promise) {
    Intent intent = new Intent();
    intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
    intent.putExtra(EXTRA_STICKER_PACK_ID, identifier);
    intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, getContentProviderAuthority(reactContext));
    intent.putExtra(EXTRA_STICKER_PACK_NAME, stickerPackName);

    try {
      Activity activity = getCurrentActivity();
      ResolveInfo should = activity.getPackageManager().resolveActivity(intent, 0);
      if (should != null) {
        activity.startActivityForResult(intent, ADD_PACK);
        promise.resolve("OK");
      } else {
        promise.resolve("OK, but not opened");
      }
    } catch (ActivityNotFoundException e) {
      promise.reject(ERROR_ADDING_STICKER_PACK, e);
    } catch  (Exception e){
      promise.reject(ERROR_ADDING_STICKER_PACK, e);
    }
  }

  @ReactMethod
  public void addStickerPack(String name, String creator, String trayImage, Promise promise) {
    try {
      String newId = UUID.randomUUID().toString();
      StickerPack sp = new StickerPack(
              newId,
              name,
              creator,
              Uri.parse(trayImage),
              "",
              "",
              "",
              "",
              reactContext.getApplicationContext());
      StickerBook.addStickerPackExisting(sp);
      promise.resolve(newId);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void addStickerToStickerPack(String identifier, String stickerUri, Promise promise) {
    StickerPack stickerPackById = StickerBook.getStickerPackById(identifier);
    Uri uri = Uri.parse(stickerUri);
    if (stickerPackById != null) {
      stickerPackById.addSticker(uri, reactContext);
      promise.resolve("OK");
    } else {
      promise.reject("404", "sticker pack not found");
    }
  }

  @ReactMethod
  public void getAllStickerPack(Promise promise) {
    ArrayList<StickerPack> stickerPacks = DataArchiver.readStickerPackJSON(reactContext);
    Gson gson = new Gson();
    String json = gson.toJson(stickerPacks);
    promise.resolve(json);
  }

  @ReactMethod
  public void createEmptyContentJSon(Promise promise) {
    boolean b = DataArchiver.writeStickerBookJSON(Collections.<StickerPack>emptyList(), reactContext);
    if (b) {
      promise.resolve("content.json created");
    } else {
      promise.reject("500", "content.json not created");
    }
  }

  @ReactMethod
  public void createContentJson(String jsonStickerPack, Promise promise) {
    Gson gson = new Gson();
    Type listType = new TypeToken<ArrayList<StickerPack>>(){}.getType();
    List<StickerPack> stickerPackList = gson.fromJson(jsonStickerPack, listType);
    boolean b = DataArchiver.writeStickerBookJSON(stickerPackList, reactContext);
    if (b) {
      promise.resolve("content.json created");
    } else {
      promise.reject("500", "content.json not created");
    }
  }

}
