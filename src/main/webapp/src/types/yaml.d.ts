declare module '*.yaml' {
  // nested objects of strings, the keys are not typed
  const value: Record<string, unknown>;
  export default value;
}
