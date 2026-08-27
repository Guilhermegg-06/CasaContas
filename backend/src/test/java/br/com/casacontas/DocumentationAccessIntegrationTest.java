package br.com.casacontas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DocumentationAccessIntegrationTest {

  @Autowired MockMvc mockMvc;

  @Test
  void exposesOpenApiAndSwaggerWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
    mockMvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }
}
