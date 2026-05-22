"use client";
import ErrorState from "../components/ErrorState/index.jsx";

export default function Error({ error, reset }) {
  return <ErrorState error={error} reset={reset} />;
}
