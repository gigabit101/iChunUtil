package me.ichun.mods.ichunutil.common.head;

import com.google.gson.*;
import me.ichun.mods.ichunutil.api.common.head.HeadInfo;
import me.ichun.mods.ichunutil.common.iChunUtil;
import me.ichun.mods.ichunutil.common.util.IOUtil;
import me.ichun.mods.ichunutil.loader.LoaderHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class HeadHandler
{
    public static final HashMap<Class<? extends LivingEntity>, String> MODEL_OFFSET_HELPERS_JSON = new HashMap<>();
    public static final HashMap<Class<? extends LivingEntity>, HeadInfo<?>> MODEL_OFFSET_HELPERS = new HashMap<>();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeAdapter(HeadInfo.class, new HeadInfo.Serializer())
            .create();
    public static final HashSet<String> IMC_HEAD_INFO = new HashSet<>();
    public static final HashSet<HeadInfo.HeadHolder> IMC_HEAD_INFO_OBJ = new HashSet<>();
    public static final int HEAD_INFO_VERSION = 5;


    public static BooleanSupplier acidEyesBooleanSupplier = () -> false;

    @Nullable
    public static HeadInfo<?> getHelper(Class<? extends LivingEntity> clz)
    {
        if(MODEL_OFFSET_HELPERS.containsKey(clz))
        {
            return MODEL_OFFSET_HELPERS.get(clz);
        }
        HeadInfo<?> helper = null;
        Class clzz = clz.getSuperclass();
        if(clzz != LivingEntity.class)
        {
            helper = getHelper(clzz);
            if(helper != null)
            {
                helper = GSON.fromJson(GSON.toJson(helper), helper.getClass());
            }
        }
        MODEL_OFFSET_HELPERS.put(clz, helper);
        return helper;
    }

    @Nullable
    public static String getHelperJson(Class<? extends LivingEntity> clz)
    {
        if(MODEL_OFFSET_HELPERS_JSON.containsKey(clz))
        {
            return MODEL_OFFSET_HELPERS_JSON.get(clz);
        }
        String json = null;
        Class clzz = clz.getSuperclass();
        if(clzz != LivingEntity.class)
        {
            json = getHelperJson(clzz);
        }
        //We don't cache on upper layers. We just want the raw for the top-most class
        return json;
    }

    private static Path headDir;

    private static boolean init;
    public static boolean hasInit() { return init; }
    public static synchronized void init() //should be initialised in FMLLoadCompleteEvent stage
    {
        if(!init)
        {
            init = true;

            HeadInfo.horseEasterEgg = () -> iChunUtil.configClient.horseEasterEgg;
            HeadInfo.acidEyesBooleanSupplier = acidEyesBooleanSupplier;
            HeadInfo.aggressiveHeadTracking = () -> iChunUtil.configClient.aggressiveHeadTracking;

            try
            {
                Path workingDir = LoaderHandler.d().getConfigDir().resolve(iChunUtil.MOD_ID);
                if(!Files.exists(workingDir)) Files.createDirectory(workingDir);

                headDir = workingDir.resolve("head");
                if(!Files.exists(headDir)) Files.createDirectory(headDir);

                File extractedMarker = new File(headDir.toFile(), HEAD_INFO_VERSION + ".extracted");
                if(!extractedMarker.exists()) //presume we haven't extracted anything yet
                {
                    InputStream in = iChunUtil.class.getResourceAsStream("/heads.zip");
                    if(in != null)
                    {
                        iChunUtil.LOGGER.info("Extracted {} Head Info files.", IOUtil.extractFiles(headDir, in, true));
                    }
                    else
                    {
                        iChunUtil.LOGGER.error("Error extracting heads.zip. InputStream was null.");
                    }

                    FileUtils.writeStringToFile(extractedMarker, "", StandardCharsets.UTF_8);
                }

                loadHeadInfos();

                //                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new SerialiserHelper());
            }
            catch(IOException e)
            {
                iChunUtil.LOGGER.error("Error initialising HeadInfo resources!");
                e.printStackTrace();
            }
        }
    }

    public static Path getHeadsDir()
    {
        return headDir;
    }

    public static int loadHeadInfos()
    {
        MODEL_OFFSET_HELPERS_JSON.clear();
        MODEL_OFFSET_HELPERS.clear();

        int count = scourDirectory(getHeadsDir().toFile());
        iChunUtil.LOGGER.info("Loaded {} HeadInfo object(s)", count);

        int modCount = 0;
        if(!IMC_HEAD_INFO.isEmpty())
        {
            for(String s : IMC_HEAD_INFO)
            {
                try
                {
                    if(readHeadInfoJson(s))
                    {
                        modCount++;
                    }
                    else
                    {
                        iChunUtil.LOGGER.error("Error reading IMC HeadInfo file: {}", s);
                    }
                }
                catch(JsonSyntaxException e)
                {
                    iChunUtil.LOGGER.error("Error reading IMC HeadInfo file: {}", s);
                    e.printStackTrace();
                }
                catch(ClassNotFoundException e)
                {
                    iChunUtil.LOGGER.error("Class not found for IMC HeadInfo file: {}", s);
                }
            }
            iChunUtil.LOGGER.info("Loaded {} IMC HeadInfo object(s)", modCount);
        }
        if(!IMC_HEAD_INFO_OBJ.isEmpty())
        {
            for(HeadInfo.HeadHolder headHolder : IMC_HEAD_INFO_OBJ)
            {
                if(LivingEntity.class.isAssignableFrom(headHolder.clz))
                {
                    MODEL_OFFSET_HELPERS.put(headHolder.clz, headHolder.info);
                }
            }
        }

        return count + modCount;
    }

    private static int scourDirectory(File dir)
    {
        int count = 0;
        File[] files = dir.listFiles();
        for(File file : files)
        {
            if(file.isDirectory())
            {
                count += scourDirectory(file);
            }
            else if(file.getName().endsWith(".json")) //oh hey we found a json
            {
                try
                {
                    String json = FileUtils.readFileToString(file, "UTF-8");
                    if(readHeadInfoJson(json))
                    {
                        count++;
                    }
                    else
                    {
                        iChunUtil.LOGGER.error("Error reading HeadInfo file, no forClass: {}", file);
                    }
                }
                catch(IOException | JsonSyntaxException e)
                {
                    iChunUtil.LOGGER.error("Error reading HeadInfo file: {}", file);
                    e.printStackTrace();
                }
                catch(ClassNotFoundException ignored){}
                //                {
                //                    iChunUtil.LOGGER.error("Class not found for HeadInfo file: {}", file);
                //                }
            }
        }
        return count;
    }


    public static boolean readHeadInfoJson(String json) throws ClassNotFoundException, JsonSyntaxException
    {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        if(jsonObject.has("forClass"))
        {
            String className = jsonObject.get("forClass").getAsString();

            Class clz = Class.forName(className);

            if(MODEL_OFFSET_HELPERS_JSON.containsKey(clz))
            {
                iChunUtil.LOGGER.warn("We already have another HeadInfo for {}", clz.getName());
            }

            MODEL_OFFSET_HELPERS_JSON.put(clz, json);

            if(!loadHeadInfo(clz, json))
            {
                MODEL_OFFSET_HELPERS_JSON.remove(clz);
            }
            return true;
        }
        return false;
    }

    public static boolean loadHeadInfo(Class clz, String json)
    {
        try
        {
            HeadInfo info = GSON.fromJson(json, HeadInfo.class);
            MODEL_OFFSET_HELPERS.put(clz, info);
            return true;
        }
        catch(Throwable t)
        {
            iChunUtil.LOGGER.error("Error deserialising HeadInfo for {}", clz.getName());
            t.printStackTrace();
        }
        return false;
    }


    //SERIALISING STUFF

    public static void serializeHeadInfos()
    {
        for(Map.Entry<Class<? extends LivingEntity>, HeadInfo<?>> e : HeadHandler.MODEL_OFFSET_HELPERS.entrySet())
        {
            if(e.getKey() == Player.class)
            {
                e.getValue().hasStrippedInfo = true;
            }

            e.getValue().forClass = e.getKey().getName();

            if(!(e.getKey() == EnderDragon.class))
            {
                continue;
            }

            File file = new File(HeadHandler.getHeadsDir().toFile(), e.getKey().getSimpleName() + ".json");
            try
            {
                String json = HeadHandler.GSON.toJson(e.getValue(), HeadInfo.class);
                FileUtils.writeStringToFile(file, json, "UTF-8");
            }
            catch(IOException ignored){}
            catch(Throwable e1)
            {
                e1.printStackTrace();
                break;
            }
        }
    }

    public static class SerialiserHelper
    {
        //        public boolean shiftKeyDown;
        //
        //        @SubscribeEvent
        //        public void onClientTick(TickEvent.ClientTickEvent event)
        //        {
        //            if(event.phase == TickEvent.Phase.END)
        //            {
        //                if(shiftKeyDown && !Screen.hasShiftDown() && Screen.hasControlDown())
        //                {
        //                    System.out.println("dump");
        //                    HeadHandler.serializeHeadInfos();
        //                }
        //                shiftKeyDown = Screen.hasShiftDown();
        //            }
        //        }
    }
}
