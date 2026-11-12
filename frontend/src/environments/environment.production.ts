const configuredUrl: string = '__PUBLIC_KEYCLOAK_URL__';
export const environment = {
  keycloakUrl: configuredUrl.startsWith('__PUBLIC_') ? `${location.origin}/auth` : configuredUrl,
} as const;
