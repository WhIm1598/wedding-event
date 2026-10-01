/** Joins truthy class names */
export const cn = (...classes: Array<string | false | null | undefined>): string => classes.filter(Boolean).join(' ');
