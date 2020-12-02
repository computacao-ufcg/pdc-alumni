package br.edu.ufcg.computacao.alumni.api.http.request;

import br.edu.ufcg.computacao.alumni.api.http.CommonKeys;
import br.edu.ufcg.computacao.alumni.api.http.response.UfcgAlumnusData;
import br.edu.ufcg.computacao.alumni.constants.ConfigurationPropertyDefaults;
import br.edu.ufcg.computacao.alumni.constants.SystemConstants;
import br.edu.ufcg.computacao.alumni.core.ApplicationFacade;
import br.edu.ufcg.computacao.alumni.core.holders.AlumniHolder;
import br.edu.ufcg.computacao.eureca.common.exceptions.EurecaException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = Alumnus.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class AlumnusTest {

    private static final String ALUMNUS_ENDPOINT = Alumnus.ENDPOINT;
    private static final String ALUMNI_NAMES_SUFIX = Alumnus.ENDPOINT + "/names";
    private static final String ALUMNI_CURRENT_JOB_SUFIX = Alumnus.ENDPOINT + "/currentJob";

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;


    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    @Test
    public void getAlumniTest() throws Exception {
        Mockito.doReturn(getFakePage()).when(this.facade).getAlumniData(Mockito.anyString(),Mockito.anyInt());
        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(ALUMNUS_ENDPOINT + "/0")
                .headers(headers).contentType(MediaType.APPLICATION_JSON)).andReturn();
        int expectedStatus = HttpStatus.OK.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        System.out.println(result.getResponse().getContentAsString());
        Mockito.verify(this.facade, Mockito.times(1)).getAlumniData(Mockito.anyString(),Mockito.anyInt());
    }

    @Test
    public void getAlumniNames() throws Exception {
        Mockito.doReturn(getFakePage()).when(this.facade).getAlumniNames(Mockito.anyString(),Mockito.anyInt());
        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(ALUMNI_NAMES_SUFIX + "/0")
                .headers(headers).contentType(MediaType.APPLICATION_JSON)).andReturn();

        int expectedStatus = HttpStatus.OK.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        System.out.println(result.getResponse().getContentAsString());
        Mockito.verify(this.facade, Mockito.times(1)).getAlumniNames(Mockito.anyString(),Mockito.anyInt());
    }

    @Test
    public void getAlumnusCurrentJob() throws Exception {
        Mockito.doReturn(getFakePage()).when(this.facade).getAlumniCurrentJob(Mockito.anyString(),Mockito.anyInt());
        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(ALUMNI_CURRENT_JOB_SUFIX + "/0")
                .headers(headers).contentType(MediaType.APPLICATION_JSON)).andReturn();

        int expectedStatus = HttpStatus.OK.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        System.out.println(result.getResponse().getContentAsString());
        Mockito.verify(this.facade, Mockito.times(1)).getAlumniCurrentJob(Mockito.anyString(),Mockito.anyInt());

    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeUserToken = "fake-access-id";
        headers.set(CommonKeys.AUTHENTICATION_TOKEN_KEY, fakeUserToken);
        return headers;
    }

    private Page getFakePage() {
        Pageable pageable= new PageRequest(0, 10);

        List list = new ArrayList<>();
        int start = (int) pageable.getOffset();
        int end = (int) ((start + pageable.getPageSize()) > list.size() ?
                list.size() : (start + pageable.getPageSize()));

        Page page = new PageImpl<>(list.subList(start, end), pageable, list.size());
        return page;

    }
}