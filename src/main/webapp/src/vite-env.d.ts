/// <reference types="vite/client" />
/// <reference types="vite-plugin-pwa/client" />

interface ImportMetaEnv {
  /** version of the build, set by Quinoa from the version of the project */
  readonly VITE_APP_VERSION?: string;
}
