package br.com.fiap.sobraboaapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

/**
 * Todo erro da API sai no formato {@link ApiError}.
 * Estende ResponseEntityExceptionHandler para que os erros do próprio Spring MVC
 * (rota inexistente 404, método não permitido 405, tipo de conteúdo 415, parâmetro faltando 400...)
 * mantenham o status correto, em vez de cair no handler genérico e virar 500.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---------------- erros do domínio ----------------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler({ConflitoException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> conflito(Exception ex, HttpServletRequest req) {
        String msg = ex instanceof ConflitoException ? ex.getMessage()
                : "Operação conflita com dados já existentes";
        return build(HttpStatus.CONFLICT, msg, req, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> regra(RegraNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, null);
    }

    // (o import correto é o do Spring Security; o java.nio.file.AccessDeniedException não é o mesmo tipo)
    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> proibido(Exception ex, HttpServletRequest req) {
        String msg = ex instanceof AcessoNegadoException ? ex.getMessage()
                : "Você não tem permissão para esta operação";
        return build(HttpStatus.FORBIDDEN, msg, req, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> credenciais(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos", req, null);
    }

    @ExceptionHandler(TentativasExcedidasException.class)
    public ResponseEntity<ApiError> tentativas(TentativasExcedidasException ex, HttpServletRequest req) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), req, null);
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<ApiError> ia(IaIndisponivelException ex, HttpServletRequest req) {
        log.error("Falha na IA", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, null);
    }

    /**
     * ?sort=campoQueNaoExiste: o Spring Data lança PropertyReferenceException (às vezes embrulhada).
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiError> acessoInvalidoAoBanco(InvalidDataAccessApiUsageException ex, HttpServletRequest req) {
        return generico(ex, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generico(Exception ex, HttpServletRequest req) {
        // Ordenação por campo inexistente (PropertyReferenceException). Detectada pelo nome da
        // classe porque o pacote dela mudou entre as versões do Spring Data.
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getClass().getSimpleName().equals("PropertyReferenceException")) {
                return build(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido na ordenação (sort)", req, null);
            }
        }
        log.error("Erro inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.", req, null);
    }

    // ---------------- erros do Spring MVC (sobrescritas) ----------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.CampoInvalido(f.getField(), f.getDefaultMessage()))
                .toList();
        return corpo(HttpStatus.BAD_REQUEST, "Dados inválidos", request, campos);
    }

    /**
     * @Max/@Min/@Positive direto em @RequestParam (ex.: raioKm, size).
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var campos = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .map(m -> new ApiError.CampoInvalido("parametro", m))
                .toList();
        return corpo(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", request, campos);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return corpo(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou malformado (confira o JSON, as datas e os valores dos enums)",
                request, null);
    }

    /**
     * Qualquer outra exceção do MVC passa por aqui, mantendo o status que o Spring escolheu.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String msg = switch (statusCode.value()) {
            case 404 -> "Rota inexistente";
            case 405 -> "Método HTTP não permitido para esta rota";
            case 406 -> "Formato de resposta não aceito";
            case 415 -> "Tipo de conteúdo não suportado (use application/json)";
            case 400 -> "Requisição inválida: " + ex.getMessage();
            default -> "Não foi possível processar a requisição";
        };
        if (statusCode.is5xxServerError()) log.error("Erro do MVC", ex);
        return corpo(statusCode, msg, request, null);
    }

    // ---------------- util ----------------

    private ResponseEntity<ApiError> build(HttpStatus s, String msg, HttpServletRequest req,
                                           List<ApiError.CampoInvalido> campos) {
        var body = new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), msg, req.getRequestURI(), campos);
        return ResponseEntity.status(s).body(body);
    }

    private ResponseEntity<Object> corpo(HttpStatusCode s, String msg, WebRequest request,
                                         List<ApiError.CampoInvalido> campos) {
        String caminho = request instanceof ServletWebRequest sw ? sw.getRequest().getRequestURI() : "";
        String razao = s instanceof HttpStatus hs ? hs.getReasonPhrase() : "Erro";
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), razao, msg, caminho, campos));
    }
}