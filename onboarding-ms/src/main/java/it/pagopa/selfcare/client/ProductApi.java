package it.pagopa.selfcare.client;

import io.quarkiverse.openapi.generator.annotations.GeneratedClass;
import io.quarkiverse.openapi.generator.annotations.GeneratedMethod;
import io.quarkiverse.openapi.generator.annotations.GeneratedParam;
import it.pagopa.selfcare.client.auth.AuthenticationPropagationHeadersFactory;
import it.pagopa.selfcare.client.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

/**
  * selc-product
  * <p>The services described in this section deal with the management of the Product entity, providing the necessary methods for its creation, consultation and activation.</p>
  */
@Path("/products")
@RegisterRestClient
@GeneratedClass(value="product.json", tag = "Product")
@RegisterClientHeaders(AuthenticationPropagationHeadersFactory.class)
@ApplicationScoped
public interface ProductApi {

    /**
     * createProduct
     *
     * Service that allows the insert of a new occurrence for the Product entity
     *
     */
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("createProductUsingPOST")
    public io.smallrye.mutiny.Uni<ProductResource> createProductUsingPOST(
        CreateProductDto createProductDto
    );

    /**
     * createSubProduct
     *
     * Service that allows the insert of a new occurrence for the Product entity
     *
     */
    @POST
    @Path("/{id}/sub-products")
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("createSubProductUsingPOST")
    public io.smallrye.mutiny.Uni<ProductResource> createSubProductUsingPOST(
        @GeneratedParam("id") @PathParam("id") String id, 
        CreateSubProductDto createSubProductDto
    );

    /**
     * deleteProduct
     *
     * Service that allows the deactivation of a specific product by an Admin user
     *
     */
    @DELETE
    @Path("/{id}")
    @Produces({"application/problem+json"})
    @GeneratedMethod ("deleteProductUsingDELETE")
    public io.smallrye.mutiny.Uni<jakarta.ws.rs.core.Response> deleteProductUsingDELETE(
        @GeneratedParam("id") @PathParam("id") String id
    );

    /**
     * getProductIsValid
     *
     * Service that returns the information for a single product given its product id
     *
     */
    @GET
    @Path("/{id}/valid")
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("getProductIsValidUsingGET")
    public io.smallrye.mutiny.Uni<ProductResource> getProductIsValidUsingGET(
        @GeneratedParam("id") @PathParam("id") String id
    );

    /**
     * getProductRoles
     *
     * Service that returns the information about mappings between Party's and Product's role
     *
     */
    @GET
    @Path("/{id}/role-mappings")
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("getProductRolesUsingGET")
    public io.smallrye.mutiny.Uni<Map<String, ProductRoleInfoRes>> getProductRolesUsingGET(
        @GeneratedParam("id") @PathParam("id") String id
    );

    /**
     * getProduct
     *
     * Service that returns the information for a single product given its product id
     *
     */
    @GET
    @Path("/{id}")
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("getProductUsingGET")
    public io.smallrye.mutiny.Uni<ProductResource> getProductUsingGET(
        @GeneratedParam("id") @PathParam("id") String id, 
        @GeneratedParam("institutionType") @QueryParam("institutionType") String institutionType
    );

    /**
     * getProductsTree
     *
     * Service that returns the list of PagoPA products tree
     *
     */
    @GET
    @Path("/tree")
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("getProductsTreeUsingGET")
    public io.smallrye.mutiny.Uni<List<ProductTreeResource>> getProductsTreeUsingGET(
    );

    /**
     * getProducts
     *
     * Service that returns the list of PagoPA products
     *
     */
    @GET
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("getProductsUsingGET")
    public io.smallrye.mutiny.Uni<List<ProductResource>> getProductsUsingGET(
    );

    /**
     * updateProductStatus
     *
     * Service that allows to update the product status
     *
     */
    @PUT
    @Path("/{id}/status/{status}")
    @Produces({"application/problem+json"})
    @GeneratedMethod ("updateProductStatusUsingPUT")
    public io.smallrye.mutiny.Uni<jakarta.ws.rs.core.Response> updateProductStatusUsingPUT(
        @GeneratedParam("id") @PathParam("id") String id, 
        @GeneratedParam("status") @PathParam("status") String status
    );

    /**
     * updateProduct
     *
     * Service that allows the update of a previously inserted occurrence of the Product entity
     *
     */
    @PUT
    @Path("/{id}")
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("updateProductUsingPUT")
    public io.smallrye.mutiny.Uni<ProductResource> updateProductUsingPUT(
        @GeneratedParam("id") @PathParam("id") String id, 
        UpdateProductDto updateProductDto
    );

    /**
     * updateSubProduct
     *
     * Service that allows the update of a previously inserted occurrence of the Product entity
     *
     */
    @PUT
    @Path("/{id}/sub-products")
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    @GeneratedMethod ("updateSubProductUsingPUT")
    public io.smallrye.mutiny.Uni<ProductResource> updateSubProductUsingPUT(
        @GeneratedParam("id") @PathParam("id") String id, 
        UpdateSubProductDto updateSubProductDto
    );

}
