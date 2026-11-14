const configuredUrl: string = '__PUBLIC_KEYCLOAK_URL__';
export const environment = {
  diagnostics: false,
  keycloakUrl: configuredUrl.startsWith('__PUBLIC_') ? `${location.origin}/auth` : configuredUrl,
} as const;
