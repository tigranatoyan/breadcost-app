import nextCoreWebVitals from "eslint-config-next/core-web-vitals";
import prettier from "eslint-config-prettier/flat";

const config = [
  ...nextCoreWebVitals,
  {
    ignores: ["FIGMA_TRIAL.tsx", "playwright-report/**", "test-results/**"],
  },
  {
    rules: {
      "no-unused-vars": ["warn", {
        argsIgnorePattern: "^_",
        varsIgnorePattern: "^_",
        caughtErrorsIgnorePattern: "^_",
      }],
      "no-console": ["warn", { allow: ["warn", "error"] }],
      "prefer-const": "error",
      "no-var": "error",
      eqeqeq: ["error", "always"],
      "@next/next/no-img-element": "off",
    },
  },
  prettier,
];

export default config;