window.addEventListener('load', function () {
  window.ui = SwaggerUIBundle({
    url: '/openapi/openapi-golden-path.yaml',
    dom_id: '#swagger-ui',
    deepLinking: true,
    displayOperationId: true,
    tryItOutEnabled: true,
    docExpansion: 'list',
    persistAuthorization: false,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    layout: 'StandaloneLayout'
  });
});
