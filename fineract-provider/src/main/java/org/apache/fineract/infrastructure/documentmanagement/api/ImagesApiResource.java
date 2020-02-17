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
package org.apache.fineract.infrastructure.documentmanagement.api;

import com.lowagie.text.pdf.codec.Base64;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.Base64EncodedImage;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.documentmanagement.contentrepository.ContentRepositoryUtils;
import org.apache.fineract.infrastructure.documentmanagement.contentrepository.ContentRepositoryUtils.IMAGE_FILE_EXTENSION;
import org.apache.fineract.infrastructure.documentmanagement.data.ImageData;
import org.apache.fineract.infrastructure.documentmanagement.exception.InvalidEntityTypeForImageManagementException;
import org.apache.fineract.infrastructure.documentmanagement.service.ImageReadPlatformService;
import org.apache.fineract.infrastructure.documentmanagement.service.ImageWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.InputStream;

@Path("{entity}/{entityId}/images")
@Component
@Scope("singleton")
@Api(value = "DomainName//api//v1//{entity}//{entityId}//images", description = "")
@RequiredArgsConstructor
public class ImagesApiResource {

    private final PlatformSecurityContext context;
    private final ImageReadPlatformService imageReadPlatformService;
    private final ImageWritePlatformService imageWritePlatformService;
    private final DefaultToApiJsonSerializer<ClientData> toApiJsonSerializer;

    /**
     * Upload images through multi-part form upload
     */
    @POST
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    public String addNewClientImage(@PathParam("entity") final String entityName, @PathParam("entityId") final Long entityId,
                                    @FormDataParam("file") final InputStream inputStream,
                                    @FormDataParam("file") final FormDataContentDisposition fileDetails) {
        validateEntityTypeforImage(entityName);
        // TODO: vishwas might need more advances validation (like reading magic
        // number) for handling malicious clients
        // and clients not setting mime type
        ContentRepositoryUtils.validateClientImageNotEmpty(fileDetails.getFileName());
        ContentRepositoryUtils.validateImageMimeType(fileDetails.getType()); // TODO: @aleks check this

        final CommandProcessingResult result = this.imageWritePlatformService.saveOrUpdateImage(entityName, entityId,
                fileDetails.getFileName(), inputStream, fileDetails.getSize());

        return this.toApiJsonSerializer.serialize(result);
    }

    /**
     * Upload image as a Data URL (essentially a base64 encoded stream)
     */
    @POST
    @Consumes({ MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String addNewClientImage(@PathParam("entity") final String entityName, @PathParam("entityId") final Long entityId,
            final String jsonRequestBody) {
        validateEntityTypeforImage(entityName);
        final Base64EncodedImage base64EncodedImage = ContentRepositoryUtils.extractImageFromDataURL(jsonRequestBody);

        final CommandProcessingResult result = this.imageWritePlatformService.saveOrUpdateImage(entityName, entityId, base64EncodedImage);

        return this.toApiJsonSerializer.serialize(result);
    }

    /**
     * Returns a base 64 encoded client image Data URI
     */
    @GET
    @Consumes({ MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.TEXT_PLAIN })
    public Response retrieveImage(@PathParam("entity") final String entityName, @PathParam("entityId") final Long entityId,
            @QueryParam("maxWidth") final Integer maxWidth, @QueryParam("maxHeight") final Integer maxHeight,
            @QueryParam("output") final String output) {
        validateEntityTypeforImage(entityName);
        if (ENTITY_TYPE_FOR_IMAGES.CLIENTS.toString().equalsIgnoreCase(entityName)) {
            this.context.authenticatedUser().validateHasReadPermission("CLIENTIMAGE");
        } else if (ENTITY_TYPE_FOR_IMAGES.STAFF.toString().equalsIgnoreCase(entityName)) {
            this.context.authenticatedUser().validateHasReadPermission("STAFFIMAGE");
        }

        if (output != null && (output.equals("octet") || output.equals("inline_octet"))) { return downloadClientImage(entityName, entityId,
                maxWidth, maxHeight, output); }

        final ImageData imageData = this.imageReadPlatformService.retrieveImage(entityName, entityId);

        // TODO: Need a better way of determining image type
        String imageDataURISuffix = ContentRepositoryUtils.IMAGE_DATA_URI_SUFFIX.JPEG.getValue();
        if (StringUtils.endsWith(imageData.getLocation(), ContentRepositoryUtils.IMAGE_FILE_EXTENSION.GIF.getValue())) {
            imageDataURISuffix = ContentRepositoryUtils.IMAGE_DATA_URI_SUFFIX.GIF.getValue();
        } else if (StringUtils.endsWith(imageData.getLocation(), ContentRepositoryUtils.IMAGE_FILE_EXTENSION.PNG.getValue())) {
            imageDataURISuffix = ContentRepositoryUtils.IMAGE_DATA_URI_SUFFIX.PNG.getValue();
        }

        final String clientImageAsBase64Text = imageDataURISuffix + Base64.encodeBytes(imageData.getContentOfSize(maxWidth, maxHeight));
        return Response.ok(clientImageAsBase64Text).build();
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public Response downloadClientImage(@PathParam("entity") final String entityName, @PathParam("entityId") final Long entityId,
            @QueryParam("maxWidth") final Integer maxWidth, @QueryParam("maxHeight") final Integer maxHeight,
            @QueryParam("output") String output) {
        validateEntityTypeforImage(entityName);
        if (ENTITY_TYPE_FOR_IMAGES.CLIENTS.toString().equalsIgnoreCase(entityName)) {
            this.context.authenticatedUser().validateHasReadPermission("CLIENTIMAGE");
        } else if (ENTITY_TYPE_FOR_IMAGES.STAFF.toString().equalsIgnoreCase(entityName)) {
            this.context.authenticatedUser().validateHasReadPermission("STAFFIMAGE");
        }

        final ImageData imageData = this.imageReadPlatformService.retrieveImage(entityName, entityId);

        final ResponseBuilder response = Response.ok(imageData.getContentOfSize(maxWidth, maxHeight));
        String dispositionType = "inline_octet".equals(output) ? "inline" : "attachment";
        response.header("Content-Disposition", dispositionType + "; filename=\"" + imageData.getEntityDisplayName()
                + IMAGE_FILE_EXTENSION.JPEG + "\"");

        // TODO: Need a better way of determining image type

        response.header("Content-Type", ContentRepositoryUtils.IMAGE_MIME_TYPE.fromFileExtension(imageData.getFileExtension()));
        return response.build();
    }

    /**
     * This method is added only for consistency with other URL patterns and for
     * maintaining consistency of usage of the HTTP "verb" at the client side
     */
    @PUT
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateClientImage(@PathParam("entity") final String entityName, @PathParam("entityId") final Long entityId,
            @FormDataParam("file") final InputStream inputStream,
            @FormDataParam("file") final FormDataContentDisposition fileDetails) {
        return addNewClientImage(entityName, entityId, inputStream, fileDetails);
    }

    /**
     * This method is added only for consistency with other URL patterns and for
     * maintaining consistency of usage of the HTTP "verb" at the client side
     * 
     * Upload image as a Data URL (essentially a base64 encoded stream)
     */
    @PUT
    @Consumes({ MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateClientImage(@PathParam("entity") final String entityName, @PathParam("entityId") final Long entityId,
            final String jsonRequestBody) {
        return addNewClientImage(entityName, entityId, jsonRequestBody);
    }

    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteClientImage(@PathParam("entity") final String entityName, @PathParam("entityId") final Long entityId) {
        validateEntityTypeforImage(entityName);
        this.imageWritePlatformService.deleteImage(entityName, entityId);
        return this.toApiJsonSerializer.serialize(CommandProcessingResult.builder().resourceId(entityId).build());
    }

    /*** Entities for document Management **/
    public static enum ENTITY_TYPE_FOR_IMAGES {
        STAFF, CLIENTS;

        @Override
        public String toString() {
            return name().toString().toLowerCase();
        }
    }

    private void validateEntityTypeforImage(final String entityName) {
        if (!checkValidEntityType(entityName)) { throw new InvalidEntityTypeForImageManagementException(entityName); }
    }

    private static boolean checkValidEntityType(final String entityType) {
        for (final ENTITY_TYPE_FOR_IMAGES entities : ENTITY_TYPE_FOR_IMAGES.values()) {
            if (entities.name().equalsIgnoreCase(entityType)) { return true; }
        }
        return false;
    }

}