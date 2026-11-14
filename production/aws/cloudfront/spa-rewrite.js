// Attach only to the default S3 behavior. API/auth behaviors have no function.
function handler(event) {
  var request = event.request;
  var path = request.uri;
  if (path === '/releases' || path.indexOf('/releases/') === 0) {
    return { statusCode: 403, statusDescription: 'Forbidden' };
  }
  if (request.method !== 'GET' && request.method !== 'HEAD') return request;
  if (path === '/api' || path.indexOf('/api/') === 0 ||
      path === '/auth' || path.indexOf('/auth/') === 0 ||
      path.indexOf('/assets/') === 0 || path.indexOf('/.well-known/') === 0) return request;
  if (path.indexOf('.') === -1) request.uri = '/index.html';
  return request;
}
