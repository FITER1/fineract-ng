/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.commands.provider;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.exception.UnsupportedCommandException;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * {@link CommandHandlerProvider} provides {@link NewCommandSourceHandler}s for a given entity and action.
 * <br>
 * <br>
 * A {@link NewCommandSourceHandler} can be registered and the annotation {@link CommandType} is used to determine
 * the entity and the action the handler is capable to process.
 *
 * @author Markus Geiss
 * @version 1.0
 * @since 15.06
 * @see NewCommandSourceHandler
 * @see CommandType
 */
@Component
@Scope("singleton")
@RequiredArgsConstructor
public class CommandHandlerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandlerProvider.class);

    private final ApplicationContext applicationContext;
    private HashMap<String, String> registeredHandlers;

    /**
     * Returns a handler for the given entity and action.<br>
     * <br>
     * Throws an {@link UnsupportedCommandException} if no handler
     * for the given entity, action combination can be found.
     * @param entity the entity to lookup the handler, must be given.
     * @param action the action to lookup the handler, must be given.
     */
    public NewCommandSourceHandler getHandler (final String entity, final String action) {
        Preconditions.checkArgument(StringUtils.isNoneEmpty(entity), "An entity must be given!");
        Preconditions.checkArgument(StringUtils.isNoneEmpty(action), "An action must be given!");

        final String key =  entity + "|" + action;
        if (!this.registeredHandlers.containsKey(key)) {
            throw new UnsupportedCommandException(key);
        }
        return (NewCommandSourceHandler)this.applicationContext.getBean(this.registeredHandlers.get(key));
    }

    @PostConstruct
    public void initializeHandlerRegistry() {
        if (this.registeredHandlers == null) {
            this.registeredHandlers = new HashMap<>();

            final String[] commandHandlerBeans = this.applicationContext.getBeanNamesForAnnotation(CommandType.class);
            if (ArrayUtils.isNotEmpty(commandHandlerBeans)) {
                for (final String commandHandlerName : commandHandlerBeans) {
                    LOGGER.debug("Register command handler '" + commandHandlerName + "' ...");
                    final CommandType commandType = this.applicationContext.findAnnotationOnBean(commandHandlerName, CommandType.class);
                    try {
                        this.registeredHandlers.put(commandType.entity() + "|" + commandType.action(), commandHandlerName);
                    } catch (final Throwable th) {
                        LOGGER.error("Unable to register command handler '" + commandHandlerName + "'!", th);
                    }
                }
            }
        }
    }
}
