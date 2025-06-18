package de.mschae23.grindenchantments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import de.mschae23.config.api.ConfigIo;
import de.mschae23.config.api.exception.ConfigException;
import de.mschae23.grindenchantments.config.ClientConfig;
import de.mschae23.grindenchantments.config.ServerConfig;
import de.mschae23.grindenchantments.config.sync.ServerConfigS2CPayload;
import de.mschae23.grindenchantments.cost.CostFunction;
import de.mschae23.grindenchantments.cost.CostFunctionType;
import de.mschae23.grindenchantments.registry.GrindEnchantmentsRegistries;
import net.minecraft.core.HolderLookup;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import de.mschae23.config.api.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import com.google.gson.JsonElement;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static de.mschae23.grindenchantments.GrindEnchantmentsClient.CLIENT_CONFIG;
import static de.mschae23.grindenchantments.registry.GrindEnchantmentsRegistries.COST_FUNCTIONS;
import static de.mschae23.grindenchantments.registry.GrindEnchantmentsRegistries.COST_FUNCTION_KEY;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(GrindEnchantmentsMod.MODID)
public class GrindEnchantmentsMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "grindenchantments";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    static ServerConfig SERVER_CONFIG = null;
    static ServerConfig LOCAL_SERVER_CONFIG = ServerConfig.DEFAULT;
    // Of course, all mentions of spells can and should be replaced with whatever your registry actually is.
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public GrindEnchantmentsMod(IEventBus modEventBus, ModContainer modContainer)
    {
        GrindEnchantmentsRegistries.init();
        CostFunctionType.init();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerRegistries);

        COST_FUNCTIONS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        if (ModList.get().isLoaded("taxfreelevels"))
        {
            LOGGER.info("Grindstone Enchantments and TaxFreelevels compatibility loaded");
        }
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
    }

    void registerRegistries(NewRegistryEvent event) {
        GrindEnchantmentsRegistries.registerRegistries(event);
    }
    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        SERVER_CONFIG = initializeServerConfig(event.getServer().registryAccess());
        SERVER_CONFIG.validateRegistryEntries(event.getServer().registryAccess());
        LOCAL_SERVER_CONFIG = SERVER_CONFIG;
    }
    @SubscribeEvent
    public void onServerStarting(ServerStoppingEvent event)
    {
        SERVER_CONFIG = null;
    }



    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void registerPayloads(final RegisterPayloadHandlersEvent event){
            final PayloadRegistrar registrar = event.registrar("1");
            registrar.configurationToClient(
                    ServerConfigS2CPayload.ID,
                    ServerConfigS2CPayload.createPacketCodec(CostFunction.createPacketCodec()),
                    (payload, context)-> {

                        GrindEnchantmentsMod.SERVER_CONFIG = payload.config();

                        if (GrindEnchantmentsClient.CLIENT_CONFIG.sync().logReceivedConfig()) {
                            LOGGER.info("Received server config: {}", GrindEnchantmentsMod.SERVER_CONFIG);
                        }
                    }
                    );
        }
        @SubscribeEvent
        public static void register(final RegisterConfigurationTasksEvent event) {
            if (event.getListener().hasChannel(ServerConfigS2CPayload.ID))
            {
                event.getListener().send(new ServerConfigS2CPayload(SERVER_CONFIG != null ? SERVER_CONFIG : LOCAL_SERVER_CONFIG));
            }
        }
    }
    public static ServerConfig getServerConfig() {
        return SERVER_CONFIG == null ?
                GrindEnchantmentsClient.getClientConfig().sync().useLocalIfUnsynced() ? LOCAL_SERVER_CONFIG : ServerConfig.DISABLED
                : SERVER_CONFIG;
    }

    public static ClientConfig getClientConfig() {
        return GrindEnchantmentsClient.getClientConfig();
    }

    public static <C extends ModConfig<C>> ModConfig.Type<C, ? extends ModConfig<C>> getConfigType(ModConfig.Type<C, ? extends ModConfig<C>>[] versions, int version) {
        for (int i = versions.length - 1; i >= 0; i--) {
            ModConfig.Type<C, ? extends ModConfig<C>> v = versions[i];

            if (version == v.version()) {
                return v;
            }
        }

        return versions[versions.length - 1];
    }

    private static <C extends ModConfig<C>> C initializeGenericConfig(Path configName, C latestDefault, Codec<ModConfig<C>> codec,
                                                                      DynamicOps<JsonElement> ops, String kind) {
        // modified version of ConfigIoImpl.initializeConfig from codec-config-api
        Path filePath = FMLPaths.CONFIGDIR.get().resolve(configName);
        C latestConfig = latestDefault;

        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            try (InputStream input = Files.newInputStream(filePath)) {
                LOGGER.info("Reading {} config.",kind);

                ModConfig<C> config = ConfigIo.decodeConfig(input, codec, ops);
                latestConfig = config.latest();

                if (config.shouldUpdate() && config.version() < latestDefault.version()) {
                    // Default OpenOptions are CREATE, TRUNCATE_EXISTING, and WRITE
                    try (OutputStream output = Files.newOutputStream(filePath);
                         OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(output))) {
                        LOGGER.info("Writing updated {} config.", kind);

                        ConfigIo.encodeConfig(writer, codec, config.latest(), ops);
                    } catch (IOException e) {
                        LOGGER.error("IO exception while trying to write updated {} config: {}",  kind, e.getLocalizedMessage());
                    } catch (ConfigException e) {
                        LOGGER.error(e.getLocalizedMessage());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("IO exception while trying to read {} config: {}",  kind, e.getLocalizedMessage());
            } catch (ConfigException e) {
                LOGGER.error(e.getLocalizedMessage());
            }
        } else {
            try {
                Files.createDirectories(filePath.getParent());

                // Write default config if the file doesn't exist
                try (OutputStream output = Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                     OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(output))) {
                    LOGGER.info( "Writing default {} config.", kind);


                    ConfigIo.encodeConfig(writer, codec, latestDefault, ops);
                }
            } catch (IOException e) {
                LOGGER.error("IO exception while trying to write {} config: {}", kind, e.getLocalizedMessage());
            } catch (ConfigException e) {
                LOGGER.error(e.getLocalizedMessage());
            }
        }
        return latestConfig;
    }

    public static ClientConfig initializeClientConfig() {
        return initializeGenericConfig(Path.of(MODID+"-client.json"), ClientConfig.DEFAULT, ClientConfig.CODEC,
                JsonOps.INSTANCE, "client");
    }

    public static ServerConfig initializeServerConfig(HolderLookup.Provider wrapperLookup) {
        return initializeGenericConfig(Path.of(MODID+"-server.json"), ServerConfig.DEFAULT, ServerConfig.CODEC,
                RegistryOps.create(JsonOps.INSTANCE, wrapperLookup), "server");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
