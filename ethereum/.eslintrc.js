module.exports = {
  env: {
    browser: false,
    es2021: true,
    mocha: true,
    node: true,
  },
  plugins: ["@typescript-eslint", "chai-friendly"],
  extends: [
    "standard",
    "plugin:prettier/recommended",
    "plugin:node/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    ecmaVersion: 12,
  },
  rules: {
    "node/no-unsupported-features/es-syntax": [
      "error",
      { ignores: ["modules"] },
    ],
    "@typescript-eslint/explicit-function-return-type": ["error"],
    "node/no-missing-import": [
      "error",
      {
        tryExtensions: [".js", ".json", ".ts", ".d.ts"],
      },
    ],
    "no-unused-vars": ["error", { varsIgnorePattern: "^_" }],
    "no-unused-expressions": "off",
    "chai-friendly/no-unused-expressions": "error",
  },
};
