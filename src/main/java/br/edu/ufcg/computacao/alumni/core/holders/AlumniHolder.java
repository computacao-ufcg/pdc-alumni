package br.edu.ufcg.computacao.alumni.core.holders;

import br.edu.ufcg.computacao.alumni.api.http.CommonKeys;
import br.edu.ufcg.computacao.alumni.api.http.response.CurrentJob;
import br.edu.ufcg.computacao.alumni.constants.*;
import br.edu.ufcg.computacao.alumni.api.http.response.UfcgAlumnusData;
import br.edu.ufcg.computacao.eureca.as.api.http.request.Token;
import br.edu.ufcg.computacao.eureca.backend.api.http.request.Alumni;
import br.edu.ufcg.computacao.eureca.backend.api.http.response.AlumniPerStudentSummary;
import br.edu.ufcg.computacao.eureca.common.constants.HttpMethod;
import br.edu.ufcg.computacao.eureca.common.exceptions.EurecaException;
import br.edu.ufcg.computacao.eureca.common.exceptions.InternalServerErrorException;
import br.edu.ufcg.computacao.eureca.common.exceptions.UnavailableProviderException;
import br.edu.ufcg.computacao.eureca.common.util.ServiceAsymmetricKeysHolder;
import br.edu.ufcg.computacao.eureca.common.util.connectivity.HttpRequestClient;
import br.edu.ufcg.computacao.eureca.common.util.connectivity.HttpResponse;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AlumniHolder extends Thread {
    private Logger LOGGER = Logger.getLogger(AlumniHolder.class);

    private static AlumniHolder instance;
    private long lastModificationDate;
    private Map<String, UfcgAlumnusData> alumni;
    private AlumniHolder() {
        this.lastModificationDate = 0;
    }

    public static AlumniHolder getInstance() {
        synchronized (AlumniHolder.class) {
            if (instance == null) {
                instance = new AlumniHolder();
            }
            return instance;
        }
    }

    public synchronized void loadAlumni() throws EurecaException {
        String backendAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.BACKEND_URL_KEY);
        String backendPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.BACKEND_PORT_KEY);
        String suffix = Alumni.ENDPOINT;
        AlumniPerStudentSummary[] alumniBasicData;

        URI uri = null;
        try {
            uri = new URI(backendAddress);
        } catch (URISyntaxException e) {
            throw new InternalServerErrorException(String.format(br.edu.ufcg.computacao.eureca.common.constants.Messages.INVALID_SERVICE_URL_S, backendAddress));
        }
        uri = UriComponentsBuilder.fromUri(uri).port(backendPort).path(suffix).build(true).toUri();

        String endpoint = uri.toString();
        HashMap<String, String> headers = new HashMap<>();
        String token = getToken();
        headers.put(CommonKeys.AUTHENTICATION_TOKEN_KEY, token);
        HttpResponse response = HttpRequestClient.doGenericRequest(HttpMethod.GET, endpoint, headers, new HashMap<>());
        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        } else {
            Gson gson = new Gson();
            alumniBasicData = gson.fromJson(response.getContent(), AlumniPerStudentSummary[].class);
            this.alumni = new HashMap<>();
            for(int i = 0; i < alumniBasicData.length; i++) {
                UfcgAlumnusData alumnus = new UfcgAlumnusData(alumniBasicData[i]);
                this.alumni.put(alumnus.getRegistration(), alumnus);
                LOGGER.debug(String.format(Messages.LOADING_ALUMNI_D_S, i, alumnus.getFullName()));
            }
        }
    }

    private synchronized String getToken() throws EurecaException {
        String username = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.USERNAME);
        String password = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PASSWORD);
        String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
        String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
        String suffix = Token.ENDPOINT;
        br.edu.ufcg.computacao.eureca.as.api.http.response.Token token = null;

        URI uri = null;
        try {
            uri = new URI(asAddress);
        } catch (URISyntaxException e) {
            throw new InternalServerErrorException(String.format(br.edu.ufcg.computacao.eureca.common.constants.Messages.INVALID_SERVICE_URL_S, asAddress));
        }
        uri = UriComponentsBuilder.fromUri(uri).port(asPort).path(suffix).build(true).toUri();

        String endpoint = uri.toString();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        Map<String, String> body = new HashMap<>();
        body.put("credentials", "{ \"" + ConfigurationPropertyKeys.USERNAME + "\" : \"" + username + "\", " +
                "\"" + ConfigurationPropertyKeys.PASSWORD + "\" : \"" + password + "\" }");
        body.put("publicKey", ServiceAsymmetricKeysHolder.getInstance().getPublicKeyString());
        HttpResponse response = HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
        if (response.getHttpCode() >= HttpStatus.SC_BAD_REQUEST) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        } else {
            Gson gson = new Gson();
            token = gson.fromJson(response.getContent(), br.edu.ufcg.computacao.eureca.as.api.http.response.Token.class);
        }
        return token.getToken();
    }

    public synchronized  Map<String, UfcgAlumnusData> getAlumniMap() {
        return this.alumni;
    }

    public synchronized Collection<UfcgAlumnusData> getAlumniData() {
        return this.alumni.values();
    }

    public synchronized Page<UfcgAlumnusData> getAlumniDataPage(int requiredPage, String admission, String graduation) {
        Pageable pageable= new PageRequest(requiredPage, 10);

        List<UfcgAlumnusData> list;
        if (admission == null && graduation == null) {
            list = new ArrayList<>(this.getAlumniData());
        } else {
            list = new ArrayList<>(this.getAlumniFilteredData(admission, graduation));
        }

        int start = (int) pageable.getOffset();
        int end = (int) ((start + pageable.getPageSize()) > list.size() ?
                list.size() : (start + pageable.getPageSize()));

        Page<UfcgAlumnusData> page = new PageImpl<>(list.subList(start, end), pageable, list.size());
        return page;
    }

    private synchronized Collection<UfcgAlumnusData> getAlumniFilteredData(String admission, String graduation) {
        Set<UfcgAlumnusData> alumni = new HashSet<>();
        alumni.addAll(filterAlumniBy("admission", admission));
        alumni.addAll(filterAlumniBy("graduation", graduation));
        return alumni;
    }

    private synchronized Collection<UfcgAlumnusData> filterAlumniBy(String key, String value) {
        List<UfcgAlumnusData> alumni = new ArrayList<>();
        for (UfcgAlumnusData alumnus : this.alumni.values()) {
            switch (key.toLowerCase()) {
                case "admission":
                    if (alumnus.getAdmission().equals(value)) {
                        alumni.add(alumnus);
                    }
                    break;
                case "graduation":
                    if (alumnus.getGraduation().equals(value)) {
                        alumni.add(alumnus);
                    }
                    break;
                default:
                    break;
            }
        }
        return alumni;
    }

    public synchronized List<String> getAlumniNames() {
        return alumni
                .values()
                .stream()
                .map(UfcgAlumnusData::getFullName)
                .collect(Collectors.toList());
    }

    public synchronized Page<String> getAlumniNamesPage(int requiredPage) {
        Pageable pageable= new PageRequest(requiredPage, 10);

        List<String> list = this.getAlumniNames();
        int start = (int) pageable.getOffset();
        int end = (int) ((start + pageable.getPageSize()) > list.size() ?
                list.size() : (start + pageable.getPageSize()));

        Page<String> page = new PageImpl<>(list.subList(start, end), pageable, list.size());
        return page;
    }

    public List<CurrentJob> getAlumniCurrentJob() {
        List<CurrentJob> alumniCurrentJob = new LinkedList<>();

        for(UfcgAlumnusData alumnus : this.alumni.values()) {
            String linkedinId = MatchesHolder.getInstance().getLinkedinId(alumnus.getRegistration());
            CurrentJob current = LinkedinDataHolder.getInstance().getAlumnusCurrentJob(alumnus.getFullName(),
                    linkedinId);
            if (!current.getCurrentJob().equals("bad match") && !current.getCurrentJob().equals("not available") &&
                                                                !current.getCurrentJob().equals("not matched")) {
                alumniCurrentJob.add(current);
            }
        }
        return alumniCurrentJob;
    }
    public Page<CurrentJob> getAlumniCurrentJobPage(int requiredPage) {
        Pageable pageable= new PageRequest(requiredPage, 10);

        List<CurrentJob> list = this.getAlumniCurrentJob();
        int start = (int) pageable.getOffset();
        int end = (int) ((start + pageable.getPageSize()) > list.size() ?
                list.size() : (start + pageable.getPageSize()));

        Page<CurrentJob> page = new PageImpl<>(list.subList(start, end), pageable, list.size());
        return page;
    }
    
    private boolean dataHasChanged(String filePath) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(Paths.get(filePath), BasicFileAttributes.class);
        long currentDate = attr.lastModifiedTime().toMillis();
        if (currentDate > this.lastModificationDate) {
            this.lastModificationDate = currentDate;
            return true;
        } else {
            return false;
        }
    }

    public synchronized String getAlumnusName(String registration) {
        return this.alumni.get(registration).getFullName();
    }

    /**
     * From time to time, updates Linkedin data with data recovered by the external scraping engine
     */
    @Override
    public void run() {
        boolean isActive = true;

        while (isActive) {
            try {
                this.loadAlumni();
            } catch (EurecaException e) {
                LOGGER.error(Messages.COULD_NOT_LOAD_ALUMNI_DATA, e);
                this.alumni = new HashMap<>();
            } finally {
                try {
                    Thread.sleep(Long.parseLong(Long.toString(TimeUnit.SECONDS.toMillis(30))));
                } catch (InterruptedException e) {
                    isActive = false;
                    LOGGER.error(Messages.THREAD_HAS_BEEN_INTERRUPTED, e);                }
            }
        }
    }
}
