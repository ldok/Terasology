/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.engine.modes.loadProcesses;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.CoreRegistry;
import org.terasology.engine.module.Module;
import org.terasology.engine.module.ModuleManager;
import org.terasology.world.block.family.BlockFamilyFactory;
import org.terasology.world.block.family.BlockFamilyFactoryRegistry;
import org.terasology.world.block.family.DefaultBlockFamilyFactoryRegistry;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;

import java.util.Set;

/**
 * @author Immortius
 */
public class RegisterBlockFamilyFactories extends SingleStepLoadProcess {
    private static final Logger logger = LoggerFactory.getLogger(RegisterBlockFamilyFactories.class);

    @Override
    public String getMessage() {
        return "Registering BlockFamilyFactories...";
    }

    @Override
    public boolean step() {
        DefaultBlockFamilyFactoryRegistry registry = new DefaultBlockFamilyFactoryRegistry();
        ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);

        for (Module module : moduleManager.getActiveModules()) {
            if (module.isCodeModule()) {
                loadFamilies(registry, module.getReflections());
            }
        }

        CoreRegistry.put(BlockFamilyFactoryRegistry.class, registry);

        return true;
    }

    private void loadFamilies(DefaultBlockFamilyFactoryRegistry registry, Reflections reflections) {
        Set<Class<?>> blockFamilyFactories = reflections.getTypesAnnotatedWith(RegisterBlockFamilyFactory.class);
        for (Class<?> blockFamilyFactory : blockFamilyFactories) {
            if (!BlockFamilyFactory.class.isAssignableFrom(blockFamilyFactory)) {
                logger.error("Cannot load {}, must be a subclass of BlockFamilyFactory", blockFamilyFactory.getSimpleName());
                continue;
            }

            RegisterBlockFamilyFactory registerInfo = blockFamilyFactory.getAnnotation(RegisterBlockFamilyFactory.class);
            String id = registerInfo.value();
            logger.debug("Registering blockFamilyFactory {}", id);
            try {
                BlockFamilyFactory newBlockFamilyFactory = (BlockFamilyFactory) blockFamilyFactory.newInstance();
                registry.setBlockFamilyFactory(id, newBlockFamilyFactory);
                logger.debug("Loaded blockFamilyFactory {}", id);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to load blockFamilyFactory {}", id, e);
            }
        }
    }
}
