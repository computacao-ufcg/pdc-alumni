package br.edu.ufcg.computacao.alumni.api.http.request;

import br.edu.ufcg.computacao.alumni.api.http.CommonKeys;
import br.edu.ufcg.computacao.alumni.api.http.response.EmployerResponse;
import br.edu.ufcg.computacao.alumni.constants.ApiDocumentation;
import br.edu.ufcg.computacao.alumni.constants.Messages;
import br.edu.ufcg.computacao.alumni.constants.SystemConstants;
import br.edu.ufcg.computacao.alumni.core.ApplicationFacade;
import br.edu.ufcg.computacao.alumni.core.models.EmployerType;
import br.edu.ufcg.computacao.eureca.common.exceptions.EurecaException;
import br.edu.ufcg.computacao.eureca.common.exceptions.InvalidParameterException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = Employer.ENDPOINT)
@Api(description = ApiDocumentation.Employers.API)
public class Employer {
    protected static final String ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "employer";

    private static final Logger LOGGER = Logger.getLogger(Employer.class);

    @RequestMapping(value = "/classified/{page}", method = RequestMethod.GET)
    @ApiOperation(value = ApiDocumentation.Employers.GET_EMPLOYER_OPERATION)
    public ResponseEntity<Page<EmployerResponse>> getClassifiedEmployers(
            @ApiParam(value = ApiDocumentation.Common.PAGE)
            @PathVariable String page,
            @ApiParam(value = ApiDocumentation.Token.AUTHENTICATION_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.AUTHENTICATION_TOKEN_KEY) String token)
            throws EurecaException {
        try {
            int p;
            try{
                p = Integer.parseInt(page);
            } catch(NumberFormatException e) {
                throw new InvalidParameterException(Messages.PAGE_MUST_BE_AN_INTEGER);
            }

            Page<EmployerResponse> employers = ApplicationFacade.getInstance().getClassifiedEmployers(token, p);
            return new ResponseEntity(employers, HttpStatus.OK);
        } catch(EurecaException e) {
            LOGGER.info(String.format(Messages.SOMETHING_WENT_WRONG, e.getMessage()), e);
            throw e;
        }

    }

    @RequestMapping(value = "/classifiedByType/{page}", method = RequestMethod.GET)
    @ApiOperation(value = ApiDocumentation.Employers.GET_EMPLOYER_BY_TYPE_OPERATION)
    public ResponseEntity<Page<EmployerResponse>> getClassifiedEmployersByType(
            @ApiParam(value = ApiDocumentation.Common.PAGE)
            @PathVariable String page,
            @ApiParam(value = ApiDocumentation.Employers.TYPE)
            @RequestParam String type,
            @ApiParam(value = ApiDocumentation.Token.AUTHENTICATION_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.AUTHENTICATION_TOKEN_KEY) String token)
            throws EurecaException {

        try {
            int p;
            EmployerType t;
            try{
                p = Integer.parseInt(page);
                t =  EmployerType.getType(type.toLowerCase());
            } catch(NumberFormatException e) {
                throw new InvalidParameterException(Messages.PAGE_MUST_BE_AN_INTEGER);

            } catch(IllegalArgumentException e) {
                throw new InvalidParameterException(Messages.TYPE_MUST_BE_AN_EMPLOYER_TYPE);
            }

            Page<EmployerResponse> employers = ApplicationFacade.getInstance().getClassifiedEmployersByType(token, p, t);
            return new ResponseEntity(employers, HttpStatus.OK);
        } catch(EurecaException e) {
            LOGGER.info(String.format(Messages.SOMETHING_WENT_WRONG, e.getMessage()), e);
            throw e;
        }
    }

    @RequestMapping(value = "/unclassified/{page}", method = RequestMethod.GET)
    @ApiOperation(value = ApiDocumentation.Employers.GET_EMPLOYERS_UNDEFINED)
    public ResponseEntity<Page<EmployerResponse>> getUnclassifiedEmployers(
            @ApiParam(value = ApiDocumentation.Common.PAGE)
            @PathVariable String page,
            @ApiParam(value = ApiDocumentation.Token.AUTHENTICATION_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.AUTHENTICATION_TOKEN_KEY) String token)
            throws EurecaException {

            try {
                int p;
                try{
                    p = Integer.parseInt(page);
                }catch(NumberFormatException e) {
                    throw new InvalidParameterException(Messages.PAGE_MUST_BE_AN_INTEGER);
                }
                Page<EmployerResponse> employers = ApplicationFacade.getInstance().getUnclassifiedEmployers(token, p);
                return new ResponseEntity(employers, HttpStatus.OK);

            } catch (EurecaException e) {
                LOGGER.info(String.format(Messages.SOMETHING_WENT_WRONG, e.getMessage()), e);
                throw e;
            }
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ApiOperation(value = ApiDocumentation.Employers.DELETE_EMPLOYER_TYPE)
    public ResponseEntity<Void> deleteEmployerType(
            @ApiParam(value = ApiDocumentation.Linkedin.LINKEDIN_ID_PARAMETER)
            @RequestParam String linkedinId,
            @ApiParam(value = ApiDocumentation.Token.AUTHENTICATION_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.AUTHENTICATION_TOKEN_KEY) String token)
            throws EurecaException {

        try {
            ApplicationFacade.getInstance().setEmployerTypeToUndefined(token, linkedinId);
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (EurecaException e) {
            LOGGER.info(String.format(Messages.SOMETHING_WENT_WRONG, e.getMessage()), e);
            throw e;
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ApiOperation(value = ApiDocumentation.Employers.SET_EMPLOYER_TYPE)
    public ResponseEntity<Void> setEmployerType(
            @ApiParam(value = ApiDocumentation.Employers.TYPE)
            @RequestParam String type,
            @ApiParam(value = ApiDocumentation.Linkedin.LINKEDIN_ID_PARAMETER)
            @RequestParam String linkedinId,
            @ApiParam(value = ApiDocumentation.Token.AUTHENTICATION_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.AUTHENTICATION_TOKEN_KEY) String token)
            throws EurecaException {

        try {
            EmployerType t;
            try {
                t = EmployerType.getType(type.toLowerCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterException(Messages.TYPE_MUST_BE_AN_EMPLOYER_TYPE);
            }
            ApplicationFacade.getInstance().setEmployerType(token, t, linkedinId);
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (EurecaException e) {
            LOGGER.info(String.format(Messages.SOMETHING_WENT_WRONG, e.getMessage()), e);
            throw e;
        }
    }
}
